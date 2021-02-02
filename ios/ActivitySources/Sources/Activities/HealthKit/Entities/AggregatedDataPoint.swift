import Foundation

// Structure for aggregated values of
// - activeEnergyBurned with unit kilocalorie
// - stepCount with unit count
// - distanceCycling with unit meters
// - distanceWalkingRunning with unit meters
// Data aggregated by 1m granulariry
struct AggregatedDataPoint: Codable {
    var value: Double
    var start: Date
}
