package com.fjuul.sdk.activitysources.entities.internal.healthconnect

/**
 * Represents aggregated health data from Health Connect ready for upload.
 */
data class HCUploadData(
    val steps: List<HCStepsDataPoint> = emptyList(),
    val heartRate: List<HCHeartRateDataPoint> = emptyList(),
    val restingHeartRate: List<HCRestingHeartRateDataPoint> = emptyList(),
    val totalCalories: List<HCTotalCaloriesDataPoint> = emptyList(),
    val activeCalories: List<HCActiveCaloriesDataPoint> = emptyList(),
    val height: List<HCHeightDataPoint> = emptyList(),
    val weight: List<HCWeightDataPoint> = emptyList()
) {
    val isEmpty: Boolean
        get() = steps.isEmpty() &&
                heartRate.isEmpty() &&
                restingHeartRate.isEmpty() &&
                totalCalories.isEmpty() &&
                activeCalories.isEmpty() &&
                height.isEmpty() &&
                weight.isEmpty()

    companion object {
        fun empty() = HCUploadData()
    }
}
