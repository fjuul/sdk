package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.internal.healthconnect.data.HealthConnectRecords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant

/**
 * HealthConnectService is a wrapper around [HealthConnectClient] to:
 * - read initial historical records
 * - fetch delta changes via change tokens
 *
 * This service works with [FitnessMetricsType] and maps them to proper [Record] classes via [FitnessRecordMapper].
 * It returns [HealthConnectRecords] containing both the records and the latest sync token.
 */
class HealthConnectService(private val context: Context) {
    private val client: HealthConnectClient = HealthConnectClient.getOrCreate(context)

    /**
     * Reads historical records for the given [recordTypes] over the past [days].
     * Also returns a change token for incremental sync.
     */
    suspend fun getInitialRecords(
        recordTypes: Set<FitnessMetricsType>,
        days: Int
    ): HealthConnectRecords = withContext(Dispatchers.IO) {
        val types = recordTypes.mapNotNull { FitnessRecordMapper.getRecordClass(it) }.toSet()
        val startTime = Instant.now().minusSeconds((24 * 60 * 60 * days).toLong())
        val filter = TimeRangeFilter.after(startTime)
        val allRecords = mutableListOf<Record>()
        for (type in types) {
            allRecords += client.readRecords(ReadRecordsRequest(type, filter)).records
        }
        val changesToken = client.getChangesToken(ChangesTokenRequest(types))
        HealthConnectRecords(changesToken, allRecords)
    }

    /**
     * Reads records from a previously issued [token] using Health Connect's change API.
     * This supports incremental updates since the last sync.
     */
    suspend fun getChangeRecords(
        recordTypes: Set<FitnessMetricsType>,
        token: String
    ): HealthConnectRecords = withContext(Dispatchers.IO) {
        val targetTypes = recordTypes.mapNotNull { FitnessRecordMapper.getRecordClass(it) }.toSet()
        val records = mutableListOf<Record>()
        var nextToken = token

        do {
            val response = client.getChanges(nextToken)
            if (response.changesTokenExpired) throw IOException("Changes token has expired")

            response.changes.forEach { change ->
                when (change) {
                    is UpsertionChange -> {
                        if (change.record::class in targetTypes) {
                            records.add(change.record)
                        }
                    }
                    is DeletionChange -> {
                        // Currently ignored
                    }
                }
            }
            nextToken = response.nextChangesToken
        } while (response.hasMore)

        HealthConnectRecords(nextToken, records)
    }
}
