package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.activitysources.utils.roundTo
import com.fjuul.sdk.core.entities.IStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.reflect.KClass

/**
 * Handles synchronization of Health Connect data:
 * 1. Intraday: per-minute cumulative (calories) and statistical (heart rate) buckets, uploaded per day.
 * 2. Daily: aggregated daily steps and resting heart rate.
 * 3. Profile: latest height (cm) and weight (kg).
 */
class HealthConnectDataManager(
    private val client: HealthConnectClient,
    private val service: ActivitySourcesService,
    private val storage: IStorage,
    private val zone: ZoneOffset = ZoneOffset.UTC,
) {

    companion object {
        const val HEART_RATE_CHANGES_TOKEN = "HEART_RATE_CHANGES_TOKEN"
        const val TOTAL_CALORIES_CHANGES_TOKEN = "TOTAL_CALORIES_CHANGES_TOKEN"
        const val ACTIVE_CALORIES_CHANGES_TOKEN = "ACTIVE_CALORIES_CHANGES_TOKEN"
        const val RESTING_HEART_RATE_CHANGES_TOKEN = "RESTING_HEART_RATE_CHANGES_TOKEN"
        const val STEPS_CHANGES_TOKEN = "STEPS_CHANGES_TOKEN"
        const val HEIGHT_CHANGES_TOKEN = "HEIGHT_CHANGES_TOKEN"
        const val WEIGHT_CHANGES_TOKEN = "WEIGHT_CHANGES_TOKEN"
        private const val TWENTY_NINE_DAYS = 29L
        private const val THIRTY_DAYS = 30L
        private const val ZERO = 0
        private const val TWO = 2
        private const val EMPTY = ""
    }

    private val myScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Synchronize intraday data: fetches 1-minute buckets over the last 2 days for all requested metrics,
     * groups them by local date, and uploads each day as a separate payload.
     *
     * @param options contains options that we need to sync
     * @param lowerDateBoundary contains first date of application sync
     * @throws HealthConnectException.NoMetricsSelectedException if no metrics are selected or on upload error.
     */
    suspend fun syncIntraday(options: HealthConnectSyncOptions, lowerDateBoundary: Date) {
        if (options.metrics.isEmpty()) throw HealthConnectException.NoMetricsSelectedException()

        var heartRateTimeChanges = listOf<HealthConnectTimeInterval>()
        // get heartRate changesToken from our storage
        var storedHeartRateChangesToken = storage.get(HEART_RATE_CHANGES_TOKEN)
        val heartRateMetric =
            setOf(FitnessMetricsType.INTRADAY_HEART_RATE).flatMap { it.toAggregateMetrics() }
                .toSet()
        if (options.metrics.contains(FitnessMetricsType.INTRADAY_HEART_RATE)) {
            // if our heartRate changesToken is empty we need to make Full Sync of daily sync.
            // After that if everything is good we get last changes token from Health Connect and
            // save it.
            if (storedHeartRateChangesToken.isNullOrEmpty()) {
                makeFullSync(heartRateMetric, lowerDateBoundary, true) {
                    myScope.launch {
                        val heartRateChangesToken = client.getChangesToken(
                            ChangesTokenRequest(recordTypes = setOf(HeartRateRecord::class))
                        )
                        storage.set(HEART_RATE_CHANGES_TOKEN, heartRateChangesToken)
                    }
                }
            }

            // If our changes token from storage is not empty we need to get time changes list.
            // method getTimeChangesList has callback when onChangesTokenExpired.
            if (!storedHeartRateChangesToken.isNullOrEmpty()) {
                heartRateTimeChanges = getTimeChangesList(
                    token = storedHeartRateChangesToken,
                    type = FitnessMetricsType.INTRADAY_HEART_RATE,
                    onTokenSave = { changedToken ->
                        storedHeartRateChangesToken = changedToken
                    },
                    onChangesTokenExpired = {
                        tokenExpired(
                            recordTypes = setOf(HeartRateRecord::class),
                            metrics = heartRateMetric,
                            changesTokenKey = HEART_RATE_CHANGES_TOKEN,
                            lowerDateBoundary = lowerDateBoundary,
                            isIntradaySync = true
                        )
                    },
                )
            }
        }

        val activeCaloriesTimeChanges = mutableListOf<HealthConnectTimeInterval>()
        val totalCaloriesTimeChanges = mutableListOf<HealthConnectTimeInterval>()
        var storedActiveCaloriesChangesToken = storage.get(ACTIVE_CALORIES_CHANGES_TOKEN)
        var storedTotalCaloriesChangesToken = storage.get(TOTAL_CALORIES_CHANGES_TOKEN)
        val activeCaloriesMetric = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
        val totalCaloriesMetric = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
        if (options.metrics.contains(FitnessMetricsType.INTRADAY_CALORIES)) {
            if (storedActiveCaloriesChangesToken.isNullOrEmpty()) {
                makeFullSync(activeCaloriesMetric, lowerDateBoundary, true) {
                    myScope.launch {
                        val activeCaloriesChangesToken = client.getChangesToken(
                            ChangesTokenRequest(recordTypes = setOf(ActiveCaloriesBurnedRecord::class))
                        )
                        storage.set(ACTIVE_CALORIES_CHANGES_TOKEN, activeCaloriesChangesToken)
                    }

                }
            }

            if (!storedActiveCaloriesChangesToken.isNullOrEmpty()) {
                activeCaloriesTimeChanges.addAll(
                    getTimeChangesList(
                        token = storedActiveCaloriesChangesToken,
                        type = FitnessMetricsType.INTRADAY_CALORIES,
                        onTokenSave = { changedToken ->
                            storedActiveCaloriesChangesToken = changedToken
                        },
                        onChangesTokenExpired = {
                            tokenExpired(
                                recordTypes = setOf(ActiveCaloriesBurnedRecord::class),
                                metrics = activeCaloriesMetric,
                                changesTokenKey = ACTIVE_CALORIES_CHANGES_TOKEN,
                                lowerDateBoundary = lowerDateBoundary,
                                isIntradaySync = true
                            )
                        },
                        isActiveCaloriesBurned = true,
                    )
                )
            }

            if (storedTotalCaloriesChangesToken.isNullOrEmpty()) {
                makeFullSync(totalCaloriesMetric, lowerDateBoundary, true) {
                    myScope.launch {
                        val totalCaloriesChangesToken = client.getChangesToken(
                            ChangesTokenRequest(recordTypes = setOf(TotalCaloriesBurnedRecord::class))
                        )
                        storage.set(TOTAL_CALORIES_CHANGES_TOKEN, totalCaloriesChangesToken)
                    }
                }
            }
            if (!storedTotalCaloriesChangesToken.isNullOrEmpty()) {
                totalCaloriesTimeChanges.addAll(
                    getTimeChangesList(
                        token = storedTotalCaloriesChangesToken,
                        type = FitnessMetricsType.INTRADAY_CALORIES,
                        onTokenSave = { changedToken ->
                            storedTotalCaloriesChangesToken = changedToken
                        },
                        onChangesTokenExpired = {
                            tokenExpired(
                                recordTypes = setOf(TotalCaloriesBurnedRecord::class),
                                metrics = totalCaloriesMetric,
                                changesTokenKey = TOTAL_CALORIES_CHANGES_TOKEN,
                                lowerDateBoundary = lowerDateBoundary,
                                isIntradaySync = true,
                            )
                        },
                        isActiveCaloriesBurned = false,
                    )
                )
            }
        }

        syncIntradayChangedBuckets(heartRateMetric, heartRateTimeChanges) {
            storage.set(HEART_RATE_CHANGES_TOKEN, storedHeartRateChangesToken)
        }

        syncIntradayChangedBuckets(activeCaloriesMetric, activeCaloriesTimeChanges) {
            storage.set(ACTIVE_CALORIES_CHANGES_TOKEN, storedActiveCaloriesChangesToken)
        }

        syncIntradayChangedBuckets(totalCaloriesMetric, totalCaloriesTimeChanges) {
            storage.set(TOTAL_CALORIES_CHANGES_TOKEN, storedTotalCaloriesChangesToken)
        }
    }

    /**
     * Synchronize daily summary data: fetch today’s steps and resting heart rate,
     * build payload entries for each day, and upload if there is any data.
     *
     * @param options contains the set of metrics to sync
     * @param lowerDateBoundary contains first date of application sync
     * @throws HealthConnectException if no metrics are selected or upload fails
     */
    suspend fun syncDaily(options: HealthConnectSyncOptions, lowerDateBoundary: Date) {
        if (options.metrics.isEmpty()) throw HealthConnectException.NoMetricsSelectedException()

        var restingHeartRateTimeChanges = listOf<HealthConnectTimeInterval>()
        var restingHeartRateChangesToken = storage.get(RESTING_HEART_RATE_CHANGES_TOKEN)
        val heartRateMetric =
            setOf(FitnessMetricsType.RESTING_HEART_RATE).flatMap { it.toAggregateMetrics() }
                .toSet()
        if (options.metrics.contains(FitnessMetricsType.RESTING_HEART_RATE)) {
            if (restingHeartRateChangesToken.isNullOrEmpty()) {
                restingHeartRateChangesToken = client.getChangesToken(
                    ChangesTokenRequest(recordTypes = setOf(RestingHeartRateRecord::class))
                )

                makeFullSync(heartRateMetric, lowerDateBoundary, false) {
                    storage.set(RESTING_HEART_RATE_CHANGES_TOKEN, restingHeartRateChangesToken)
                }
            }

            restingHeartRateTimeChanges =
                getTimeChangesList(
                    token = restingHeartRateChangesToken,
                    type = FitnessMetricsType.RESTING_HEART_RATE,
                    onTokenSave = { changedToken ->
                        restingHeartRateChangesToken = changedToken
                    },
                    onChangesTokenExpired = {
                        tokenExpired(
                            recordTypes = setOf(RestingHeartRateRecord::class),
                            metrics = heartRateMetric,
                            changesTokenKey = RESTING_HEART_RATE_CHANGES_TOKEN,
                            lowerDateBoundary = lowerDateBoundary,
                            isIntradaySync = false
                        )
                    },
                )
        }

        val stepsMetric =
            setOf(FitnessMetricsType.STEPS).flatMap { it.toAggregateMetrics() }
                .toSet()
        var stepsCountTimeChanges = listOf<HealthConnectTimeInterval>()
        var stepsChangesToken = storage.get(STEPS_CHANGES_TOKEN)
        if (options.metrics.contains(FitnessMetricsType.STEPS)) {
            if (stepsChangesToken.isNullOrEmpty()) {
                stepsChangesToken = client.getChangesToken(
                    ChangesTokenRequest(recordTypes = setOf(StepsRecord::class))
                )

                makeFullSync(stepsMetric, lowerDateBoundary, false) {
                    storage.set(STEPS_CHANGES_TOKEN, stepsChangesToken)
                }
            }

            stepsCountTimeChanges = getTimeChangesList(
                token = stepsChangesToken,
                type = FitnessMetricsType.STEPS,
                onTokenSave = { changedToken ->
                    stepsChangesToken = changedToken
                },
                onChangesTokenExpired = {
                    tokenExpired(
                        recordTypes = setOf(StepsRecord::class),
                        metrics = stepsMetric,
                        changesTokenKey = STEPS_CHANGES_TOKEN,
                        lowerDateBoundary = lowerDateBoundary,
                        isIntradaySync = false
                    )
                },
            )
        }

        if (restingHeartRateTimeChanges.isNotEmpty()) {
            syncDailyChangedBuckets(heartRateMetric, restingHeartRateTimeChanges) {
                storage.set(RESTING_HEART_RATE_CHANGES_TOKEN, restingHeartRateChangesToken)
            }
        }

        if (stepsCountTimeChanges.isNotEmpty()) {
            syncDailyChangedBuckets(stepsMetric, stepsCountTimeChanges) {
                storage.set(STEPS_CHANGES_TOKEN, stepsChangesToken)
            }
        }
    }

    /**
     * Makes full sync when token expires and saves last changes token in our storage.
     *
     * @param recordTypes contains type of records that we need to collect
     * @param metrics contains the set of metrics to sync
     * @param changesTokenKey contains key of token that we need to save in storage
     * @param lowerDateBoundary contains first date of application sync
     * @param isIntradaySync Boolean variable that get us is it intraday sync or not
     */
    private fun tokenExpired(
        recordTypes: Set<KClass<out Record>>,
        metrics: Set<AggregateMetric<*>>,
        changesTokenKey: String,
        lowerDateBoundary: Date,
        isIntradaySync: Boolean,
    ) {
        // If token expired we need to make full sync and save our last changes token when
        // everything is success
        myScope.launch {
            val changesToken = client.getChangesToken(
                ChangesTokenRequest(
                    recordTypes = recordTypes
                )
            )

            makeFullSync(metrics, lowerDateBoundary, isIntradaySync) {
                storage.set(
                    changesTokenKey,
                    changesToken
                )
            }
        }
    }

    /**
     * Get list of time changes intervals.
     *
     * @param token contains last saved token
     * @param type contains metric that we need to get
     * @param onTokenSave callback when needs to save last changesToken
     * @param onChangesTokenExpired callback when changesToken expired
     */
    private suspend fun getTimeChangesList(
        token: String,
        type: FitnessMetricsType,
        onTokenSave: (String) -> Unit,
        onChangesTokenExpired: () -> Unit,
        isActiveCaloriesBurned: Boolean? = null,
    ): MutableList<HealthConnectTimeInterval> {
        val timeChangesList = mutableListOf<HealthConnectTimeInterval>()
        var nextChangesToken = token
        do {
            val response = client.getChanges(nextChangesToken)
            if (response.changesTokenExpired) {
                onChangesTokenExpired()
                return mutableListOf()
            }
            response.changes.forEach { change ->
                when (change) {
                    is UpsertionChange -> {
                        when (val record = change.record) {
                            is HeartRateRecord -> {
                                if (type == FitnessMetricsType.INTRADAY_HEART_RATE) {
                                    timeChangesList.add(
                                        HealthConnectTimeInterval(
                                            record.startTime.toUTC(),
                                            record.endTime.toUTC(),
                                        ),
                                    )
                                }
                            }

                            is ActiveCaloriesBurnedRecord -> {
                                if (type == FitnessMetricsType.INTRADAY_CALORIES && isActiveCaloriesBurned == true) {
                                    timeChangesList.add(
                                        HealthConnectTimeInterval(
                                            record.startTime.toUTC(),
                                            record.endTime.toUTC(),
                                        ),
                                    )
                                }
                            }

                            is TotalCaloriesBurnedRecord -> {
                                if (type == FitnessMetricsType.INTRADAY_CALORIES && isActiveCaloriesBurned == false) {
                                    timeChangesList.add(
                                        HealthConnectTimeInterval(
                                            record.startTime.toUTC(),
                                            record.endTime.toUTC(),
                                        ),
                                    )
                                }
                            }

                            is RestingHeartRateRecord -> {
                                if (type == FitnessMetricsType.RESTING_HEART_RATE) {
                                    timeChangesList.add(
                                        HealthConnectTimeInterval(
                                            record.time.toUTC(),
                                            record.time.toUTC(),
                                        ),
                                    )
                                }
                            }

                            is StepsRecord -> {
                                if (type == FitnessMetricsType.STEPS) {
                                    timeChangesList.add(
                                        HealthConnectTimeInterval(
                                            record.startTime.toUTC(),
                                            record.endTime.toUTC(),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            nextChangesToken = response.nextChangesToken
        } while (response.hasMore)

        onTokenSave(nextChangesToken)

        return timeChangesList.toSet().toMutableList()
    }

    /**
     * Get list of time changes intervals.
     *
     * @param metrics contains last saved token
     * @param timeChanges contains metric that we need to get
     * @param onSuccess callback when needs to save last changesToken
     */
    private suspend fun syncIntradayChangedBuckets(
        metrics: Set<AggregateMetric<*>>,
        timeChanges: List<HealthConnectTimeInterval>,
        onSuccess: () -> Unit,
    ) {
        // we group time changes by days with startTime
        if (timeChanges.isNotEmpty()) {
            timeChanges
                .groupBy { it.startTime.atZone(zone).toLocalDate().toString() }
                .forEach { (_, dayTimeChanges) ->
                    // why set? because time changes can intersect. And when we set them into "set",
                    // all the same buckets delete.
                    val changedBuckets = mutableSetOf<AggregationResultGroupedByDuration>()
                    // we collect all changed buckets during each day
                    dayTimeChanges.forEach {
                        val startTime =
                            it.startTime.atZone(zone).toLocalDateTime().withSecond(ZERO)
                                .withNano(ZERO)
                                .toInstant(zone)
                        val endTime =
                            it.endTime.plus(Duration.ofMinutes(1)).atZone(zone).toLocalDateTime()
                                .withSecond(ZERO).withNano(ZERO)
                                .toInstant(zone)
                        val buckets: List<AggregationResultGroupedByDuration> =
                            client.aggregateGroupByDuration(
                                AggregateGroupByDurationRequest(
                                    metrics = metrics,
                                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                                    timeRangeSlicer = Duration.ofMinutes(1)
                                )
                            )
                        changedBuckets.addAll(buckets)
                    }

                    // and upload this buckets to server
                    uploadIntradayBuckets(changedBuckets.toMutableList())
                }
        }
        onSuccess()
    }

    /**
     * Make a full from first day of register app or during 30 days. Why 30 days? Because time of
     * changeToken expiration is 30 days.
     *
     * @param metrics contains metrics for HealthConnect that we need to sync
     * @param lowerDateBoundary contains first date of application sync
     * @param isIntradaySync boolean variable returns true if it is intraday sync
     * @param onSuccess callback when needs to save last changesToken
     */
    private suspend fun makeFullSync(
        metrics: Set<AggregateMetric<*>>,
        lowerDateBoundary: Date,
        isIntradaySync: Boolean,
        onSuccess: () -> Unit,
    ) {
        // makes seconds and milliseconds 0
        val now = Instant.now().atZone(zone).toLocalDateTime().withSecond(ZERO).withNano(ZERO)
            .toInstant(zone)
        // For daily sync we need to make sync during 29 days because HealthConnect returns us not
        // full aggregate data for first day
        val minusDays = if (isIntradaySync) THIRTY_DAYS else TWENTY_NINE_DAYS
        val thirtyDaysAgo = now.minus(Duration.ofDays(minusDays))
        var start = lowerDateBoundary.toInstant().coerceAtLeast(thirtyDaysAgo)

        // create a list of days
        val days = mutableListOf<Instant>()
        while (start.truncatedTo(ChronoUnit.DAYS) < now.truncatedTo(ChronoUnit.DAYS)) {
            days.add(start)
            start = start.plus(Duration.ofDays(1))
        }
        days.add(now)

        // make sync day by day because HealthConnect has 5000 buckets limit and if there will be
        // more than 5000 buckets - we will get an exception
        for (i in 0..<days.size) {
            // Read 1-minute buckets by every day
            if (isIntradaySync) {
                val startTimeLocalDate = days[i].atZone(zone).toLocalDate().atStartOfDay()
                val startTime = startTimeLocalDate
                    .toInstant(zone)
                    // never request intraday data from before the point in time of the tracker connection;
                    // this coercion is only relevant for the first day of the sync interval
                    .coerceAtLeast(lowerDateBoundary.toInstant().truncatedTo(ChronoUnit.MINUTES))
                val endTime = startTimeLocalDate.plusDays(1).toInstant(zone)
                val buckets: List<AggregationResultGroupedByDuration> =
                    client.aggregateGroupByDuration(
                        AggregateGroupByDurationRequest(
                            metrics = metrics,
                            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                            timeRangeSlicer = Duration.ofMinutes(1)
                        )
                    )
                if (buckets.isNotEmpty()) {
                    uploadIntradayBuckets(buckets)
                }
            } else {
                // Read aggregate data day by day
                val todayStart = LocalDate.now().atStartOfDay()
                val localZoneOffset = ZoneId.systemDefault().rules.getOffset(todayStart)
                val startTime = days[i].atZone(localZoneOffset).toLocalDate().atStartOfDay()
                val endTime = startTime.plusDays(1)
                val buckets: List<AggregationResultGroupedByPeriod> = client.aggregateGroupByPeriod(
                    AggregateGroupByPeriodRequest(
                        metrics = metrics,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                        timeRangeSlicer = Period.ofDays(1)
                    )
                )
                uploadDailyBucketsByPeriod(buckets) {}
            }
        }

        onSuccess()
    }

    /**
     * Uploads intraday buckets. Get buckets, sort them by time, find total calories, active
     * calories, heart rate and upload them if they are not empty
     *
     * @param buckets list of updated buckets
     */
    private fun uploadIntradayBuckets(buckets: List<AggregationResultGroupedByDuration>) {
        // Group by date and upload
        buckets
            .toSet()
            .sortedBy { it.startTime }
            .groupBy { it.startTime.atZone(zone).toLocalDate().toString() }
            .forEach { (_, dayBuckets) ->
                // Collect all distinct data origins for this day
                val origins = dayBuckets
                    .flatMap { it.result.dataOrigins.map { od -> od.packageName } }
                    .distinct()

                // Build cumulative calorie entries
                val totalEntries = dayBuckets.mapNotNull { b ->
                    b.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
                        ?.inKilocalories
                        ?.roundTo(2)
                        ?.let { ValueEntry(b.startTime.toString(), it) }
                }
                val activeEntries = dayBuckets.mapNotNull { bucket ->
                    bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                        ?.inKilocalories
                        ?.roundTo(2)
                        ?.let { ValueEntry(bucket.startTime.toString(), it) }
                }

                // Build statistical heart-rate entries
                val hrEntries = dayBuckets.mapNotNull { b ->
                    val min = b.result[HeartRateRecord.BPM_MIN]?.toDouble()
                    val avg = b.result[HeartRateRecord.BPM_AVG]?.toDouble()
                    val max = b.result[HeartRateRecord.BPM_MAX]?.toDouble()
                    if (min != null || avg != null || max != null) {
                        HeartRateEntry(
                            start = b.startTime.toString(),
                            min = min ?: 0.0,
                            avg = avg ?: 0.0,
                            max = max ?: 0.0
                        )
                    } else null
                }

                // Skip if nothing to send
                if (!(totalEntries.isEmpty() && activeEntries.isEmpty() && hrEntries.isEmpty())) {
                    // Assemble payload
                    val payload = HealthConnectIntradayPayload(
                        totalCalories = totalEntries.takeIf { it.isNotEmpty() }
                            ?.let { MetricData(origins, it) },
                        activeCalories = activeEntries.takeIf { it.isNotEmpty() }
                            ?.let { MetricData(origins, it) },
                        heartrate = hrEntries.takeIf { it.isNotEmpty() }
                            ?.let { MetricData(origins, it) }
                    )

                    // Upload and check for errors
                    service.uploadHealthConnectIntraday(payload)
                        .execute()
                        .let {
                            if (it.isError) {
                                it.error?.let { error ->
                                    throw error
                                }
                            }
                        }
                }

            }
    }

    /**
     * Sync daily changed buckets.
     *
     * @param metrics contains metrics for HealthConnect that we need to sync
     * @param timeChangesIntervals contains metric that we need to get
     * @param onSuccess callback when upload did successful
     */
    private suspend fun syncDailyChangedBuckets(
        metrics: Set<AggregateMetric<*>>,
        timeChangesIntervals: List<HealthConnectTimeInterval>,
        onSuccess: () -> Unit,
    ) {
        val timeChangesDays = getTimeChangesDays(timeChangesIntervals)
        // Request daily aggregates (1-day buckets) from Health Connect
        timeChangesDays.forEach {
            val todayStartLocal = LocalDate.now().atStartOfDay()
            val localZoneOffset = ZoneId.systemDefault().rules.getOffset(todayStartLocal)
            val todayStart = it.atZone(localZoneOffset).toLocalDateTime()
            val tomorrowStart =
                todayStart.plus(Duration.ofDays(1)).atZone(localZoneOffset).toLocalDateTime()
            val buckets: List<AggregationResultGroupedByPeriod> = client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(todayStart, tomorrowStart),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )

            uploadDailyBucketsByPeriod(buckets.toSet().toList(), onSuccess)
        }
    }

    /**
     * Upload daily buckets by day period.
     *
     * @param buckets contains buckets that we need to upload
     * @param onSuccess callback when upload did successful
     */
    private fun uploadDailyBucketsByPeriod(
        buckets: List<AggregationResultGroupedByPeriod>,
        onSuccess: () -> Unit,
    ) {
        // Transform each non-empty bucket into a DailyEntry
        val entries = buckets.mapNotNull { b ->
            // Extract step count, if available
            val steps = b.result[StepsRecord.COUNT_TOTAL]

            // Extract resting heart rate stats, if any values present
            val minHr = b.result[RestingHeartRateRecord.BPM_MIN]?.toDouble()
            val avgHr = b.result[RestingHeartRateRecord.BPM_AVG]?.toDouble()
            val maxHr = b.result[RestingHeartRateRecord.BPM_MAX]?.toDouble()
            val hrEntry = if (minHr != null || avgHr != null || maxHr != null) {
                HeartRateEntry(start = null, min = minHr, avg = avgHr, max = maxHr)
            } else null

            if (steps == null && hrEntry == null) return@mapNotNull null

            DailyEntry(
                date = b.startTime.toLocalDate().toString(),
                dataOrigins = b.result.dataOrigins.map { it.packageName },
                steps = steps,
                restingHeartRate = hrEntry
            )
        }

        if (entries.isNotEmpty()) {
            service.uploadHealthConnectDailies(HealthConnectDailiesPayload(entries))
                .execute()
                .let {
                    if (it.isError) {
                        it.error?.let { error ->
                            throw error
                        }
                    }
                }
        }

        onSuccess()
    }

    /**
     * Synchronize profile data: reads up to 30 days of height and weight records,
     * picks the most recent values and uploads them.
     * @throws HealthConnectException.NoMetricsSelectedException if no metrics are selected or on upload error.
     */
    suspend fun syncProfile(options: HealthConnectSyncOptions, lowerDateBoundary: Date) {
        if (options.metrics.isEmpty()) throw HealthConnectException.NoMetricsSelectedException()

        val now = Instant.now()
        val thirtyDaysAgo = now.minus(THIRTY_DAYS, ChronoUnit.DAYS)
        val startTime = lowerDateBoundary.toInstant().coerceAtLeast(thirtyDaysAgo)

        val storedHeightChangesToken = storage.get(HEIGHT_CHANGES_TOKEN) ?: EMPTY
        val storedWeightChangesToken = storage.get(WEIGHT_CHANGES_TOKEN) ?: EMPTY

        // Read height. If changes token in storage is empty than makes full sync for 30 days
        // If changes token is not empty than makes changes token mechanism
        var heightChangesToken = EMPTY
        val heightsList = if (options.metrics.contains(FitnessMetricsType.HEIGHT)) {
            if (storedHeightChangesToken.isEmpty()) {
                makeFullHeightSync(startTime) {
                    heightChangesToken = it
                }
            } else {
                var nextChangesToken = storedHeightChangesToken
                val heightList = mutableListOf<HeightRecord>()
                do {
                    val response = client.getChanges(nextChangesToken)
                    if (response.changesTokenExpired) {
                        makeFullHeightSync(startTime) {
                            heightChangesToken = it
                        }
                    }
                    response.changes.forEach { change ->
                        when (change) {
                            is UpsertionChange -> {
                                when (val record = change.record) {
                                    is HeightRecord -> {
                                        heightList.add(record)
                                    }
                                }
                            }
                        }
                    }
                    nextChangesToken = response.nextChangesToken
                } while (response.hasMore)
                heightChangesToken = nextChangesToken
                heightList
            }
        } else null

        // Read weight. If changes token in storage is empty than makes full sync for 30 days
        // If changes token is not empty than makes changes token mechanism
        var weightChangesToken = EMPTY
        val weightsList = if (options.metrics.contains(FitnessMetricsType.WEIGHT)) {
            if (storedWeightChangesToken.isEmpty()) {
                makeFullWeightSync(startTime) {
                    weightChangesToken = it
                }
            } else {
                var nextChangesToken = storedWeightChangesToken
                val weightList = mutableListOf<WeightRecord>()
                do {
                    val response = client.getChanges(nextChangesToken)
                    if (response.changesTokenExpired) {
                        makeFullWeightSync(startTime) {
                            weightChangesToken = it
                        }
                    }
                    response.changes.forEach { change ->
                        when (change) {
                            is UpsertionChange -> {
                                when (val record = change.record) {
                                    is WeightRecord -> {
                                        weightList.add(record)
                                    }
                                }
                            }
                        }
                    }
                    nextChangesToken = response.nextChangesToken
                } while (response.hasMore)
                weightChangesToken = nextChangesToken
                weightList
            }
        } else null

        // Pick latest measurements
        val latestHeightCm = heightsList
            ?.maxByOrNull { it.time }
            ?.height
            ?.inMeters
            ?.times(100.0)
            ?.roundTo(TWO)

        val latestWeightKg = weightsList
            ?.maxByOrNull { it.time }
            ?.weight
            ?.inKilograms
            ?.roundTo(TWO)

        if (!(latestHeightCm == null && latestWeightKg == null)) {
            service.uploadHealthConnectProfile(
                HealthConnectProfilePayload(height = latestHeightCm, weight = latestWeightKg)
            )
                .execute()
                .let {
                    if (it.isError) {
                        it.error?.let { error ->
                            throw error
                        }
                    }
                }
        }

        storage.set(HEIGHT_CHANGES_TOKEN, heightChangesToken)
        storage.set(WEIGHT_CHANGES_TOKEN, weightChangesToken)
    }

    private suspend fun makeFullHeightSync(
        startTime: Instant,
        onTokenSave: (String) -> Unit
    ): List<HeightRecord> {
        val heightChangesToken = client.getChangesToken(
            ChangesTokenRequest(recordTypes = setOf(HeightRecord::class))
        )
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = HeightRecord::class,
                timeRangeFilter = TimeRangeFilter.after(startTime)
            )
        ).records
        onTokenSave(heightChangesToken)
        return records
    }

    private suspend fun makeFullWeightSync(
        startTime: Instant,
        onTokenSave: (String) -> Unit
    ): List<WeightRecord> {
        val weightChangesToken = client.getChangesToken(
            ChangesTokenRequest(recordTypes = setOf(WeightRecord::class))
        )
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.after(startTime)
            )
        ).records

        onTokenSave(weightChangesToken)
        return records
    }

    private fun getTimeChangesDays(timeChangesIntervals: List<HealthConnectTimeInterval>): List<Instant> {
        val timeChangesDays = mutableSetOf<Instant>()
        timeChangesIntervals.forEach {
            val startTimeIntervalByDay =
                it.startTime.atZone(zone).toLocalDate().atStartOfDay().atZone(zone).toInstant()
            val endTimeIntervalByDay =
                it.endTime.atZone(zone).toLocalDate().atStartOfDay().atZone(zone).toInstant()
            timeChangesDays.add(startTimeIntervalByDay)
            timeChangesDays.add(endTimeIntervalByDay)
        }

        return timeChangesDays.toMutableList()
    }
}

/**
 * Extension to map fitness metrics enum to Health Connect AggregateMetric.
 */
private fun FitnessMetricsType.toAggregateMetrics(): Set<AggregateMetric<*>> = when (this) {
    FitnessMetricsType.INTRADAY_CALORIES -> setOf(
        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
    )

    FitnessMetricsType.INTRADAY_HEART_RATE -> setOf(
        HeartRateRecord.BPM_MIN,
        HeartRateRecord.BPM_AVG,
        HeartRateRecord.BPM_MAX,
    )

    FitnessMetricsType.RESTING_HEART_RATE -> setOf(
        RestingHeartRateRecord.BPM_MIN,
        RestingHeartRateRecord.BPM_AVG,
        RestingHeartRateRecord.BPM_MAX,
    )

    FitnessMetricsType.STEPS -> setOf(StepsRecord.COUNT_TOTAL)
    else -> throw HealthConnectException.UnsupportedMetricException(this.name)
}

private fun Instant.toUTC(): Instant {
    val zone = ZoneOffset.UTC
    return this.atZone(zone).toInstant()
}
