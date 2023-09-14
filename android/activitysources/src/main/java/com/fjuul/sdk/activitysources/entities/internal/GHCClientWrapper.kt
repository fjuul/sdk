package com.fjuul.sdk.activitysources.entities.internal

import android.content.Context
import android.os.Build
//TODO: Replace Log with Timber and use SDK's Timber utility classes
import android.util.Log
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
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.io.IOException
import java.time.Instant
import java.util.concurrent.CompletableFuture

// The minimum android level that can use Health Connect
const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

class GHCClientWrapper(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val LOGTAG = "GHCClientWrapper"
    private val recordTypes = setOf(
        ExerciseSessionRecord::class,
        StepsRecord::class,
        TotalCaloriesBurnedRecord::class,
        HeartRateRecord::class,
        HeightRecord::class,
        WeightRecord::class
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

    fun getDefaultRequiredPermissions() : Set<String> {
        return setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
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
        GlobalScope.future { hasAllPermissions(permissions)}

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
    fun getInitialRecordsAsync(days: Int) : CompletableFuture<HealthConnectRecords> =
        GlobalScope.future { getInitialRecords(days) }

    private suspend fun getInitialRecords(days: Int) : HealthConnectRecords {
        val startTime = Instant.now().minusSeconds((24*60*60*days).toLong())
        val filter = TimeRangeFilter.after(startTime)
        val records = mutableListOf<Record>()
        for (type in recordTypes) {
            records.addAll(healthConnectClient.readRecords(
                ReadRecordsRequest(type, timeRangeFilter = filter)).records)

        }
        val changesToken = healthConnectClient.getChangesToken(ChangesTokenRequest(recordTypes))
        return HealthConnectRecords(changesToken, records)
     }

    /**
     * Provide a way for Java to (indirectly) call our Kotlin suspend function.
     * TODO: handle coroutines better in production code
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun getChangesAsync(token: String): CompletableFuture<HealthConnectRecords> =
        GlobalScope.future { getChanges(token) }

    /**
     * Retrieve changes from a changes token. For now, ignore deletions
     */
    private suspend fun getChanges(token: String): HealthConnectRecords {
        var nextChangesToken = token
        val records = mutableListOf<Record>()
        do {
            val response = healthConnectClient.getChanges(nextChangesToken)
            if (response.changesTokenExpired) {
                //TODO handle expired change token, e.g. fetch last 30 days' data
                throw IOException("Changes token has expired")
            }
            for (change in response.changes) {
                if (change is UpsertionChange) {
                    records.add(change.record)
                } else if (change is DeletionChange) {
                    //TODO Handle deletions
                    Log.i(LOGTAG, "Ignoring a deletion change")
                } else {
                    Log.e(LOGTAG, "Unexpected change type")
                }
            }
            nextChangesToken = response.nextChangesToken
        } while (response.hasMore)
        return HealthConnectRecords(nextChangesToken, records)
    }
}

data class HealthConnectRecords (val nextToken: String, val records : List<Record>)

/**
 * Health Connect requires that the underlying Health Connect APK is installed on the device.
 * [HealthConnectAvailability] represents whether this APK is indeed installed, whether it is not
 * installed but supported on the device, or whether the device is not supported (based on Android
 * version).
 */
enum class HealthConnectAvailability {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED
}
