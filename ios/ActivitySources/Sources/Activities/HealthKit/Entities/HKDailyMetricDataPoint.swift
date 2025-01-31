import Foundation

struct HKDailyMetricDataPoint: Codable {
    var date: Date
    var steps: Int?
    var restingHeartRate: Int?
}
