package com.fjuul.sdk.activitysources.entities.internal.healthconnect.data

import androidx.health.connect.client.records.Record

data class HealthConnectRecords(
    val nextToken: String,
    val records: List<Record>
)
