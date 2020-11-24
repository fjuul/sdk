import Foundation

// Structure for aggregated values of
// - activeEnergyBurned with unit kilocalorie
// - stepCount with unit count
// - distanceCycling
// - distanceWalkingRunning
// Data aggregated by 1m granulariry

struct HeartRateDataPoint: Codable {
    var start: Date
    var avg: Double
    var min: Double
    var max: Double
}
