package com.fjuul.sdk.activitysources.entities.internal

import android.content.Context
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.core.utils.Logger
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.io.IOException
import java.time.Instant
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

// The minimum android level that can use Health Connect
const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

/**
 * A wrapper for methods in the Kotlin Health Connect API which wraps the Kotlin
 * 'suspend' methods with GlobalScope.future to make them callable from Java. See
 * API guide here: https://developer.android.com/health-and-fitness/guides/health-connect
 */
class HCClientWrapper(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val profileRecordTypes = setOf(
        HeightRecord::class,
        WeightRecord::class,
    )
    private val intradayRecordTypes = setOf(
        StepsRecord::class,
        TotalCaloriesBurnedRecord::class,
        HeartRateRecord::class,
    )
    private val sessionRecordTypes = setOf(
        ExerciseSessionRecord::class,
    )

    var availability = mutableStateOf(HealthConnectAvailability.NOT_SUPPORTED)
        private set

    init {
        checkAvailability()
    }

    @ChecksSdkIntAtLeast(api = MIN_SUPPORTED_SDK)
    fun checkAvailability() {
        availability.value = when {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.INSTALLED
            Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    fun getDefaultRequiredPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(PowerRecord::class),
            HealthPermission.getReadPermission(SpeedRecord::class),
            HealthPermission.getReadPermission(HeightRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
        )
    }

    /**
     * Provide a way for Java to (indirectly) call our Kotlin suspend function.
     * TODO: handle coroutines better in production code
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun hasAllPermissionsAsync(permissions: Set<String>): CompletableFuture<Boolean> =
        GlobalScope.future { hasAllPermissions(permissions) }

    /**
     * Determines whether all the specified permissions are already granted. It is recommended to
     * call [PermissionController.getGrantedPermissions] first in the permissions flow, as if the
     * permissions are already granted then there is no need to request permissions via
     * [PermissionController.createRequestPermissionResultContract].
     */
    suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    /**
     * Provide a way for Java to (indirectly) call our Kotlin suspend function.
     * TODO: handle coroutines better in production code
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun getInitialProfileRecordsAsync(days: Int, optionsMetricTypes: Set<FitnessMetricsType>): CompletableFuture<HealthConnectRecords> =
        GlobalScope.future { getInitialRecords(profileRecordTypes, optionsMetricTypes, days) }

    @OptIn(DelicateCoroutinesApi::class)
    fun getInitialIntradayRecordsAsync(days: Int, optionsMetricTypes: Set<FitnessMetricsType>): CompletableFuture<HealthConnectRecords> =
        GlobalScope.future { getInitialRecords(intradayRecordTypes, optionsMetricTypes, days) }

    private suspend fun getInitialRecords(
        recordTypes: Set<KClass<out Record>>,
        optionsMetricTypes: Set<FitnessMetricsType>,
        days: Int,
    ): HealthConnectRecords {
        val startTime = Instant.now().minusSeconds((24 * 60 * 60 * days).toLong())
        val filter = TimeRangeFilter.after(startTime)
        val records = mutableListOf<Record>()
        for (type in recordTypes.intersect(toRecordTypes(optionsMetricTypes))) {
            records.addAll(
                healthConnectClient.readRecords(
                    ReadRecordsRequest(type, timeRangeFilter = filter),
                ).records,
            )
        }
        // TODO: https://developer.android.com/health-and-fitness/guides/health-connect/common-workflows/sync-data
        // recommends getting separate tokens per data type.
        val changesToken = healthConnectClient.getChangesToken(ChangesTokenRequest(recordTypes))
        return HealthConnectRecords(changesToken, records)
    }

    /**
     * Provide a way for Java to (indirectly) call our Kotlin suspend function.
     * TODO: handle coroutines better in production code
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun getProfileChangeRecordsAsync(token: String, optionsMetricTypes: Set<FitnessMetricsType>): CompletableFuture<HealthConnectRecords> =
        GlobalScope.future { getChangeRecords(profileRecordTypes.intersect(toRecordTypes(optionsMetricTypes)), token) }

    @OptIn(DelicateCoroutinesApi::class)
    fun getIntradayChangeRecordsAsync(token: String, optionsMetricTypes: Set<FitnessMetricsType>): CompletableFuture<HealthConnectRecords> =
        GlobalScope.future { getChangeRecords(intradayRecordTypes.intersect(toRecordTypes(optionsMetricTypes)), token) }

    /**
     * Retrieve changes from a changes token. For now, ignore deletions
     */
    private suspend fun getChangeRecords(recordTypes: Set<KClass<out Record>>, token: String): HealthConnectRecords {
        var nextChangesToken = token
        val records = mutableListOf<Record>()
        do {
            val response = healthConnectClient.getChanges(nextChangesToken)
            if (response.changesTokenExpired) {
                // TODO handle expired change token, e.g. fetch last 30 days' data
                throw IOException("Changes token has expired")
            }
            for (change in response.changes) {
                if (change is UpsertionChange) {
                    // Only add changes of the types that were requested
                    for (recordType in recordTypes) {
                        if (change.record::class == recordType) {
                            records.add(change.record)
                            break
                        }
                    }
                } else if (change is DeletionChange) {
                    // TODO Handle deletions
                    Logger.get().i("Ignoring a deletion change")
                } else {
                    Logger.get().e("Unexpected change type")
                }
            }
            nextChangesToken = response.nextChangesToken
        } while (response.hasMore)
        return HealthConnectRecords(nextChangesToken, records)
    }

    private fun toRecordTypes(metricTypes: Set<FitnessMetricsType>): Set<KClass<out Record>> {
        return metricTypes.map { metricType -> getRecordType(metricType) }.toSet()
    }

    private fun getRecordType(metricType: FitnessMetricsType): KClass<out Record> {
        return when (metricType) {
            FitnessMetricsType.HEIGHT -> HeightRecord::class
            FitnessMetricsType.WEIGHT -> WeightRecord::class
            FitnessMetricsType.INTRADAY_STEPS -> StepsRecord::class
            FitnessMetricsType.INTRADAY_CALORIES -> TotalCaloriesBurnedRecord::class
            FitnessMetricsType.INTRADAY_HEART_RATE -> HeartRateRecord::class
            FitnessMetricsType.WORKOUTS -> ExerciseSessionRecord::class
        }
    }
}

data class HealthConnectRecords(val nextToken: String, val records: List<Record>)

/**
 * Health Connect requires that the underlying Health Connect APK is installed on the device.
 * [HealthConnectAvailability] represents whether this APK is indeed installed, whether it is not
 * installed but supported on the device, or whether the device is not supported (based on Android
 * version).
 */
enum class HealthConnectAvailability {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED,
}
