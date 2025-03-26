package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import androidx.health.connect.client.records.*
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import kotlin.reflect.KClass

object FitnessRecordMapper {

    fun getRecordClass(metricType: FitnessMetricsType): KClass<out Record>? {
        return when (metricType) {
            FitnessMetricsType.HEIGHT -> HeightRecord::class
            FitnessMetricsType.WEIGHT -> WeightRecord::class
            FitnessMetricsType.INTRADAY_STEPS -> StepsRecord::class
            FitnessMetricsType.INTRADAY_CALORIES -> TotalCaloriesBurnedRecord::class
            FitnessMetricsType.INTRADAY_HEART_RATE -> HeartRateRecord::class
            else -> null
        }
    }
}
