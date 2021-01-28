import Foundation

// Structure for aggregated HR values of
// - avg, max, min values
// Data aggregated by 1m granulariry
struct HeartRateDataPoint: Codable {
    var start: Date
    var avg: Double
    var min: Double
    var max: Double
}
