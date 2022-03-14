import Foundation

enum HKRequestData {
    case batchData(HKBatchData)
    case dailyMetricData([HKDailyMetricDataPoint])
    case userProfileData(HKUserProfileData)
}
