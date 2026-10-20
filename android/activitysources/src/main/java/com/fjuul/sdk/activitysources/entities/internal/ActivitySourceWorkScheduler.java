package com.fjuul.sdk.activitysources.entities.internal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.workers.GFIntradaySyncWorker;
import com.fjuul.sdk.activitysources.workers.GFSessionsSyncWorker;
import com.fjuul.sdk.activitysources.workers.GFSyncWorker;
import com.fjuul.sdk.activitysources.workers.HCDailySyncWorker;
import com.fjuul.sdk.activitysources.workers.HCIntradaySyncWorker;
import com.fjuul.sdk.activitysources.workers.HCProfileSyncWorker;
import com.fjuul.sdk.activitysources.workers.ProfileSyncWorker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AtomicFile;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class ActivitySourceWorkScheduler {
    public static final String GF_INTRADAY_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.gf_intraday_sync";
    public static final String GF_SESSIONS_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.gf_sessions_sync";
    public static final String PROFILE_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.profile_sync";
    public static final String HC_INTRADAY_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.hc_intraday_sync";
    private static final String HC_DAILY_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.hc_daily_sync";
    private static final String HC_PROFILE_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.hc_profile_sync";
    private static final String SNAPSHOT_DIRECTORY = "fjuul_activity_source_work_snapshots";
    private static final String INPUT_SNAPSHOT_TAG_PREFIX = "com.fjuul.sdk.activity_source_snapshot:";

    @NonNull
    private final WorkManager workManager;
    @NonNull
    private final String userToken;
    @NonNull
    private final String userSecret;
    @NonNull
    private final String apiKey;
    @NonNull
    private final String baseUrl;
    @Nullable
    private final File snapshotDirectory;
    private final Map<String, SnapshotRecord> previousSnapshots = new HashMap<>();
    private final Map<String, String> preparedSnapshotIds = new HashMap<>();
    private volatile boolean gfIntradaySyncWorkEnqueued = false;
    private volatile boolean gfSessionsSyncWorkEnqueued = false;
    private volatile boolean profileSyncWorkEnqueued = false;
    private volatile boolean hcIntradaySyncWorkEnqueued = false;
    private volatile boolean hcDailySyncWorkEnqueued = false;
    private volatile boolean hcProfileSyncWorkEnqueued = false;

    public ActivitySourceWorkScheduler(@NonNull WorkManager workManager,
        @NonNull String userToken,
        @NonNull String userSecret,
        @NonNull String apiKey,
        @NonNull String baseUrl) {
        this(workManager, userToken, userSecret, apiKey, baseUrl, (File) null);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ActivitySourceWorkScheduler(@NonNull WorkManager workManager,
        @NonNull String userToken,
        @NonNull String userSecret,
        @NonNull String apiKey,
        @NonNull String baseUrl,
        @NonNull Context applicationContext) {
        this(workManager, userToken, userSecret, apiKey, baseUrl, snapshotDirectory(applicationContext));
    }

    private ActivitySourceWorkScheduler(@NonNull WorkManager workManager,
        @NonNull String userToken,
        @NonNull String userSecret,
        @NonNull String apiKey,
        @NonNull String baseUrl,
        @Nullable File snapshotDirectory) {
        this.workManager = workManager;
        this.userToken = userToken;
        this.userSecret = userSecret;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.snapshotDirectory = snapshotDirectory;
    }

    public static void cancelWorks(@NonNull WorkManager workManager) {
        RuntimeException failure = null;
        for (final String workName : workNames()) {
            try {
                await(workManager.cancelUniqueWork(workName));
            } catch (RuntimeException e) {
                failure = combineFailures(failure, e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void cancelWorks(@NonNull WorkManager workManager, @NonNull Context applicationContext) {
        RuntimeException failure = null;
        File directory = null;
        try {
            directory = snapshotDirectory(applicationContext);
        } catch (RuntimeException e) {
            failure = combineFailures(failure, e);
        }
        for (final String workName : workNames()) {
            boolean canceled = false;
            try {
                await(workManager.cancelUniqueWork(workName));
                canceled = true;
            } catch (RuntimeException e) {
                failure = combineFailures(failure, e);
            }
            if (canceled && directory != null) {
                try {
                    clearInputSnapshot(directory, workName);
                } catch (RuntimeException e) {
                    failure = combineFailures(failure, e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static List<ScheduledWork> snapshotWorks(@NonNull WorkManager workManager,
        @NonNull Context applicationContext) {
        final File directory = snapshotDirectory(applicationContext);
        final Map<String, ScheduledWork> scheduledWorks = new LinkedHashMap<>();
        for (final String workName : workNames()) {
            final List<WorkInfo> workInfos = await(workManager.getWorkInfosForUniqueWork(workName));
            workInfos.stream()
                .filter(workInfo -> workInfo.getState() == WorkInfo.State.ENQUEUED
                    || workInfo.getState() == WorkInfo.State.RUNNING
                    || workInfo.getState() == WorkInfo.State.BLOCKED)
                .findFirst()
                .ifPresent(
                    workInfo -> readInputSnapshot(directory, workName, workInfo.getTags()).ifPresent(inputData -> {
                        final WorkInfo.PeriodicityInfo periodicityInfo = workInfo.getPeriodicityInfo();
                        if (periodicityInfo != null) {
                            scheduledWorks.put(workName,
                                new ScheduledWork(workName,
                                    inputData,
                                    workInfo.getConstraints(),
                                    workInfo.getInitialDelayMillis(),
                                    periodicityInfo.getRepeatIntervalMillis(),
                                    periodicityInfo.getFlexIntervalMillis()));
                        }
                    }));
        }
        return Collections.unmodifiableList(new ArrayList<>(scheduledWorks.values()));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void restoreWorks(@NonNull WorkManager workManager,
        @NonNull Context applicationContext,
        @NonNull List<ScheduledWork> scheduledWorks) {
        final File directory = snapshotDirectory(applicationContext);
        RuntimeException failure = null;
        for (final String workName : workNames()) {
            try {
                await(workManager.cancelUniqueWork(workName));
                clearInputSnapshot(directory, workName);
            } catch (RuntimeException e) {
                failure = combineFailures(failure, e);
            }
        }
        for (final ScheduledWork work : scheduledWorks) {
            try {
                final PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(workerClass(work.name),
                    work.repeatIntervalMillis,
                    TimeUnit.MILLISECONDS,
                    work.flexIntervalMillis,
                    TimeUnit.MILLISECONDS).setConstraints(work.constraints)
                        .setInitialDelay(work.initialDelayMillis, TimeUnit.MILLISECONDS)
                        .setInputData(work.inputData)
                        .addTag(storeInputSnapshot(directory, work.name, work.inputData))
                        .build();
                await(workManager.enqueueUniquePeriodicWork(work.name, ExistingPeriodicWorkPolicy.REPLACE, request));
            } catch (RuntimeException e) {
                clearInputSnapshot(directory, work.name);
                failure = combineFailures(failure, e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final class ScheduledWork {
        private final String name;
        private final Data inputData;
        private final Constraints constraints;
        private final long initialDelayMillis;
        private final long repeatIntervalMillis;
        private final long flexIntervalMillis;

        private ScheduledWork(String name,
            Data inputData,
            Constraints constraints,
            long initialDelayMillis,
            long repeatIntervalMillis,
            long flexIntervalMillis) {
            this.name = name;
            this.inputData = inputData;
            this.constraints = constraints;
            this.initialDelayMillis = initialDelayMillis;
            this.repeatIntervalMillis = repeatIntervalMillis;
            this.flexIntervalMillis = flexIntervalMillis;
        }
    }

    public synchronized void cancelWorks() {
        cancelGFIntradaySyncWork();
        cancelGFSessionsSyncWork();
        cancelProfileSyncWork();
        cancelHCIntradaySyncWork();
        cancelHCDailySyncWork();
        cancelHCProfileSyncWork();
    }

    @SuppressLint("NewApi")
    public synchronized void scheduleGFIntradaySyncWork(@NonNull Set<FitnessMetricsType> intradayMetrics) {
        if (gfIntradaySyncWorkEnqueued) {
            return;
        }
        final String[] serializedIntradayMetrics = serializeFitnessMetrics(intradayMetrics);
        final Data inputWorkRequestData = buildEssentialInputData()
            .putStringArray(GFIntradaySyncWorker.KEY_INTRADAY_METRICS_ARG, serializedIntradayMetrics)
            .build();
        final PeriodicWorkRequest periodicWorkRequest =
            buildPeriodicWorkRequest(GFIntradaySyncWorker.class, GF_INTRADAY_SYNC_WORK_NAME, inputWorkRequestData);
        enqueue(GF_INTRADAY_SYNC_WORK_NAME, periodicWorkRequest);
        gfIntradaySyncWorkEnqueued = true;
    }

    public synchronized void cancelGFIntradaySyncWork() {
        cancel(GF_INTRADAY_SYNC_WORK_NAME);
        gfIntradaySyncWorkEnqueued = false;
    }

    public synchronized void scheduleGFSessionsSyncWork(@NonNull Duration minSessionDuration) {
        if (gfSessionsSyncWorkEnqueued) {
            return;
        }
        final String serializedDuration = minSessionDuration.toString();
        final Data inputWorkRequestData =
            buildEssentialInputData().putString(GFSessionsSyncWorker.KEY_MIN_SESSION_DURATION_ARG, serializedDuration)
                .build();
        final PeriodicWorkRequest periodicWorkRequest =
            buildPeriodicWorkRequest(GFSessionsSyncWorker.class, GF_SESSIONS_SYNC_WORK_NAME, inputWorkRequestData);
        enqueue(GF_SESSIONS_SYNC_WORK_NAME, periodicWorkRequest);
        gfSessionsSyncWorkEnqueued = true;
    }

    public synchronized void cancelGFSessionsSyncWork() {
        cancel(GF_SESSIONS_SYNC_WORK_NAME);
        gfSessionsSyncWorkEnqueued = false;
    }

    public synchronized void scheduleProfileSyncWork(@NonNull Set<FitnessMetricsType> profileMetrics) {
        if (profileSyncWorkEnqueued) {
            return;
        }
        final String[] serializedMetrics = serializeFitnessMetrics(profileMetrics);
        final Data inputWorkRequestData =
            buildEssentialInputData().putStringArray(ProfileSyncWorker.KEY_PROFILE_METRICS_ARG, serializedMetrics)
                .build();
        final PeriodicWorkRequest periodicWorkRequest =
            buildPeriodicWorkRequest(ProfileSyncWorker.class, PROFILE_SYNC_WORK_NAME, inputWorkRequestData);
        enqueue(PROFILE_SYNC_WORK_NAME, periodicWorkRequest);
        profileSyncWorkEnqueued = true;
    }

    public synchronized void cancelProfileSyncWork() {
        cancel(PROFILE_SYNC_WORK_NAME);
        profileSyncWorkEnqueued = false;
    }

    @SuppressLint("NewApi")
    public synchronized void scheduleHCIntradaySyncWork(@NonNull Set<FitnessMetricsType> intradayMetrics) {
        if (hcIntradaySyncWorkEnqueued) {
            return;
        }

        final String[] serializedIntradayMetrics = serializeFitnessMetrics(intradayMetrics);
        final Data inputWorkRequestData = buildEssentialInputData()
            .putStringArray(HCIntradaySyncWorker.KEY_HC_INTRADAY_METRICS, serializedIntradayMetrics)
            .build();
        final PeriodicWorkRequest periodicWorkRequest =
            buildPeriodicWorkRequest(HCIntradaySyncWorker.class, HC_INTRADAY_SYNC_WORK_NAME, inputWorkRequestData);
        enqueue(HC_INTRADAY_SYNC_WORK_NAME, periodicWorkRequest);
        hcIntradaySyncWorkEnqueued = true;
    }

    public synchronized void cancelHCIntradaySyncWork() {
        cancel(HC_INTRADAY_SYNC_WORK_NAME);
        hcIntradaySyncWorkEnqueued = false;
    }

    @SuppressLint("NewApi")
    public synchronized void scheduleHCDailySyncWork(@NonNull Set<FitnessMetricsType> dailyMetrics) {
        if (hcDailySyncWorkEnqueued) {
            return;
        }

        final String[] serializedDailyMetrics = serializeFitnessMetrics(dailyMetrics);
        final Data inputWorkRequestData =
            buildEssentialInputData().putStringArray(HCDailySyncWorker.KEY_HC_DAILY_METRICS, serializedDailyMetrics)
                .build();
        final PeriodicWorkRequest periodicWorkRequest =
            buildPeriodicWorkRequest(HCDailySyncWorker.class, HC_DAILY_SYNC_WORK_NAME, inputWorkRequestData);
        enqueue(HC_DAILY_SYNC_WORK_NAME, periodicWorkRequest);
        hcDailySyncWorkEnqueued = true;
    }

    public synchronized void cancelHCDailySyncWork() {
        cancel(HC_DAILY_SYNC_WORK_NAME);
        hcDailySyncWorkEnqueued = false;
    }

    public synchronized void scheduleHCProfileSyncWork(@NonNull Set<FitnessMetricsType> hcProfileMetrics) {
        if (hcProfileSyncWorkEnqueued) {
            return;
        }
        final String[] serializedMetrics = serializeFitnessMetrics(hcProfileMetrics);
        final Data inputWorkRequestData =
            buildEssentialInputData().putStringArray(HCProfileSyncWorker.KEY_HC_PROFILE_METRICS, serializedMetrics)
                .build();
        final PeriodicWorkRequest periodicWorkRequest =
            buildPeriodicWorkRequest(HCProfileSyncWorker.class, HC_PROFILE_SYNC_WORK_NAME, inputWorkRequestData);
        enqueue(HC_PROFILE_SYNC_WORK_NAME, periodicWorkRequest);
        hcProfileSyncWorkEnqueued = true;
    }

    public synchronized void cancelHCProfileSyncWork() {
        cancel(HC_PROFILE_SYNC_WORK_NAME);
        hcProfileSyncWorkEnqueued = false;
    }

    private Data.Builder buildEssentialInputData() {
        return new Data.Builder().putString(GFSyncWorker.KEY_USER_TOKEN_ARG, userToken)
            .putString(GFSyncWorker.KEY_USER_SECRET_ARG, userSecret)
            .putString(GFSyncWorker.KEY_API_KEY_ARG, apiKey)
            .putString(GFSyncWorker.KEY_BASE_URL_ARG, baseUrl);
    }

    private Constraints buildCommonWorkConstraints() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    @SuppressLint("NewApi")
    private String[] serializeFitnessMetrics(Set<FitnessMetricsType> metrics) {
        return metrics.stream().map(Enum::toString).toArray(String[]::new);
    }

    private PeriodicWorkRequest buildPeriodicWorkRequest(Class<? extends ListenableWorker> workerClass,
        String workName,
        Data inputData) {
        final PeriodicWorkRequest.Builder builder =
            new PeriodicWorkRequest.Builder(workerClass, 1, TimeUnit.HOURS).setConstraints(buildCommonWorkConstraints())
                .setInitialDelay(1, TimeUnit.HOURS)
                .setInputData(inputData);
        final String snapshotTag = prepareInputSnapshot(workName, inputData);
        if (snapshotTag != null) {
            builder.addTag(snapshotTag);
        }
        return builder.build();
    }

    @Nullable
    private String prepareInputSnapshot(@NonNull String workName, @NonNull Data inputData) {
        if (snapshotDirectory == null) {
            return null;
        }
        previousSnapshots.put(workName, readStoredSnapshot(snapshotDirectory, workName));
        final String snapshotTag = storeInputSnapshot(snapshotDirectory, workName, inputData);
        preparedSnapshotIds.put(workName, snapshotIdFromTag(snapshotTag));
        return snapshotTag;
    }

    private void enqueue(@NonNull String workName, @NonNull PeriodicWorkRequest request) {
        final String preparedSnapshotId = preparedSnapshotIds.remove(workName);
        try {
            await(workManager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.REPLACE, request));
            if (snapshotDirectory != null) {
                cleanupSnapshotFiles(snapshotDirectory, workName, preparedSnapshotId);
            }
            previousSnapshots.remove(workName);
        } catch (RuntimeException e) {
            if (snapshotDirectory != null && previousSnapshots.containsKey(workName)) {
                writeStoredSnapshot(snapshotDirectory, workName, previousSnapshots.remove(workName));
            }
            throw e;
        }
    }

    private void cancel(@NonNull String workName) {
        await(workManager.cancelUniqueWork(workName));
        if (snapshotDirectory != null) {
            clearInputSnapshot(snapshotDirectory, workName);
        }
        previousSnapshots.remove(workName);
        preparedSnapshotIds.remove(workName);
    }

    @NonNull
    private static List<String> workNames() {
        return Arrays.asList(GF_INTRADAY_SYNC_WORK_NAME,
            GF_SESSIONS_SYNC_WORK_NAME,
            PROFILE_SYNC_WORK_NAME,
            HC_INTRADAY_SYNC_WORK_NAME,
            HC_DAILY_SYNC_WORK_NAME,
            HC_PROFILE_SYNC_WORK_NAME);
    }

    @NonNull
    private static File snapshotDirectory(@NonNull Context context) {
        final File directory = new File(context.getApplicationContext().getNoBackupFilesDir(), SNAPSHOT_DIRECTORY);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create private activity source snapshot directory");
        }
        return directory;
    }

    @NonNull
    private static AtomicFile snapshotFile(@NonNull File directory, @NonNull String workName, @NonNull String id) {
        return new AtomicFile(new File(directory, workName + "." + id + ".snapshot"));
    }

    @NonNull
    private static AtomicFile snapshotIndexFile(@NonNull File directory, @NonNull String workName) {
        return new AtomicFile(new File(directory, workName + ".current"));
    }

    @NonNull
    private static String storeInputSnapshot(@NonNull File directory,
        @NonNull String workName,
        @NonNull Data inputData) {
        final String id = UUID.randomUUID().toString();
        writeSnapshot(directory, workName, new SnapshotRecord(id, inputData.toByteArray()));
        writeSnapshotIndex(directory, workName, id);
        return INPUT_SNAPSHOT_TAG_PREFIX + id;
    }

    @NonNull
    private static String snapshotIdFromTag(@NonNull String snapshotTag) {
        if (!snapshotTag.startsWith(INPUT_SNAPSHOT_TAG_PREFIX)) {
            throw new IllegalArgumentException("Invalid activity source snapshot tag");
        }
        return snapshotTag.substring(INPUT_SNAPSHOT_TAG_PREFIX.length());
    }

    @NonNull
    private static java.util.Optional<Data> readInputSnapshot(@NonNull File directory,
        @NonNull String workName,
        @NonNull Set<String> tags) {
        for (final String tag : tags) {
            if (!tag.startsWith(INPUT_SNAPSHOT_TAG_PREFIX)) {
                continue;
            }
            final String id = tag.substring(INPUT_SNAPSHOT_TAG_PREFIX.length());
            try {
                final SnapshotRecord record = readSnapshot(directory, workName, id);
                if (record != null) {
                    return java.util.Optional.of(Data.fromByteArray(record.data));
                }
            } catch (IOException | IllegalArgumentException e) {
                return java.util.Optional.empty();
            }
        }
        return java.util.Optional.empty();
    }

    @Nullable
    private static SnapshotRecord readStoredSnapshot(@NonNull File directory, @NonNull String workName) {
        try {
            final String id = readSnapshotIndex(directory, workName);
            return id == null ? null : readSnapshot(directory, workName, id);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read activity source work snapshot", e);
        }
    }

    private static void writeStoredSnapshot(@NonNull File directory,
        @NonNull String workName,
        @Nullable SnapshotRecord snapshot) {
        if (snapshot == null) {
            deleteAtomicFile(snapshotIndexFile(directory, workName));
        } else {
            writeSnapshot(directory, workName, snapshot);
            writeSnapshotIndex(directory, workName, snapshot.id);
        }
    }

    private static void clearInputSnapshot(@NonNull File directory, @NonNull String workName) {
        cleanupSnapshotFiles(directory, workName, null);
    }

    private static void cleanupSnapshotFiles(@NonNull File directory,
        @NonNull String workName,
        @Nullable String retainedId) {
        final String prefix = workName + ".";
        final String suffix = ".snapshot";
        final File[] files = directory.listFiles((dir, name) -> name.startsWith(prefix));
        if (files != null) {
            for (final File file : files) {
                String baseName = file.getName();
                if (baseName.endsWith(".bak") || baseName.endsWith(".new")) {
                    baseName = baseName.substring(0, baseName.length() - 4);
                }
                if (!baseName.endsWith(suffix)) {
                    continue;
                }
                final String id = baseName.substring(prefix.length(), baseName.length() - suffix.length());
                if (!id.equals(retainedId)) {
                    deleteAtomicFile(new AtomicFile(new File(directory, baseName)));
                }
            }
        }
        if (retainedId == null) {
            deleteAtomicFile(snapshotIndexFile(directory, workName));
        } else {
            writeSnapshotIndex(directory, workName, retainedId);
        }
    }

    @Nullable
    private static SnapshotRecord readSnapshot(@NonNull File directory, @NonNull String workName, @NonNull String id)
        throws IOException {
        return readSnapshotFile(snapshotFile(directory, workName, id));
    }

    @Nullable
    private static SnapshotRecord readSnapshotFile(@NonNull AtomicFile file) throws IOException {
        try (DataInputStream input = new DataInputStream(file.openRead())) {
            final String id = input.readUTF();
            final int size = input.readInt();
            if (size < 0 || size > Data.MAX_DATA_BYTES) {
                throw new IOException("Invalid activity source work snapshot size");
            }
            final byte[] bytes = new byte[size];
            input.readFully(bytes);
            return new SnapshotRecord(id, bytes);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private static void writeSnapshot(@NonNull File directory,
        @NonNull String workName,
        @NonNull SnapshotRecord snapshot) {
        writeSnapshotFile(snapshotFile(directory, workName, snapshot.id), snapshot);
    }

    private static void writeSnapshotIndex(@NonNull File directory, @NonNull String workName, @NonNull String id) {
        writeSnapshotFile(snapshotIndexFile(directory, workName), new SnapshotRecord(id, new byte[0]));
    }

    private static void writeSnapshotFile(@NonNull AtomicFile file, @NonNull SnapshotRecord snapshot) {
        FileOutputStream stream = null;
        try {
            stream = file.startWrite();
            final DataOutputStream output = new DataOutputStream(stream);
            output.writeUTF(snapshot.id);
            output.writeInt(snapshot.data.length);
            output.write(snapshot.data);
            output.flush();
            file.finishWrite(stream);
        } catch (IOException e) {
            if (stream != null) {
                file.failWrite(stream);
            }
            throw new IllegalStateException("Could not persist activity source work snapshot", e);
        }
    }

    private static void deleteAtomicFile(@NonNull AtomicFile file) {
        final File baseFile = file.getBaseFile();
        file.delete();
        if (baseFile.exists()
            || new File(baseFile.getPath() + ".bak").exists()
            || new File(baseFile.getPath() + ".new").exists()) {
            throw new IllegalStateException("Could not delete activity source work snapshot");
        }
    }

    @Nullable
    private static String readSnapshotIndex(@NonNull File directory, @NonNull String workName) throws IOException {
        final SnapshotRecord record = readSnapshotFile(snapshotIndexFile(directory, workName));
        return record == null ? null : record.id;
    }

    private static final class SnapshotRecord {
        private final String id;
        private final byte[] data;

        private SnapshotRecord(String id, byte[] data) {
            this.id = id;
            this.data = data;
        }
    }

    @NonNull
    private static Class<? extends ListenableWorker> workerClass(@NonNull String workName) {
        switch (workName) {
            case GF_INTRADAY_SYNC_WORK_NAME:
                return GFIntradaySyncWorker.class;
            case GF_SESSIONS_SYNC_WORK_NAME:
                return GFSessionsSyncWorker.class;
            case PROFILE_SYNC_WORK_NAME:
                return ProfileSyncWorker.class;
            case HC_INTRADAY_SYNC_WORK_NAME:
                return HCIntradaySyncWorker.class;
            case HC_DAILY_SYNC_WORK_NAME:
                return HCDailySyncWorker.class;
            case HC_PROFILE_SYNC_WORK_NAME:
                return HCProfileSyncWorker.class;
            default:
                throw new IllegalArgumentException("Unknown activity source work: " + workName);
        }
    }

    private static void await(@NonNull Operation operation) {
        try {
            operation.getResult().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for WorkManager operation", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("WorkManager operation failed", e.getCause());
        }
    }

    @NonNull
    private static RuntimeException combineFailures(@Nullable RuntimeException original,
        @NonNull RuntimeException next) {
        if (original == null) {
            return next;
        }
        original.addSuppressed(next);
        return original;
    }

    @NonNull
    private static <T> T await(@NonNull com.google.common.util.concurrent.ListenableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for WorkManager query", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("WorkManager query failed", e.getCause());
        }
    }
}
