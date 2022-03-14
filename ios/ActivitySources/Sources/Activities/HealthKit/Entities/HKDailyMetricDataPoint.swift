import Foundation

struct HKDailyMetricDataPoint: Codable {
    var date: Date
    var steps: Double?
    var restingHeartRate: Double?
}
