package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.content.Context
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import kotlin.reflect.KClass

object HealthConnectDataReader {

    suspend fun <T : Record> readRecords(
        context: Context,
        recordType: KClass<T>,
        startTime: Instant,
        endTime: Instant
    ): List<T>? {
        val client = HealthConnectClientProvider.getClient(context) ?: return null
        val request = ReadRecordsRequest(
            recordType = recordType,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return client.readRecords(request).records
    }
}
