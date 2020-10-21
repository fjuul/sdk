import Foundation

// Structure for aggregated values of
// - activeEnergyBurned with unit kilocalorie
// - stepCount
// - distanceCycling
// - distanceWalkingRunning
// Data aggregated by 1m granulariry

struct AggregatedDataPoint: Codable {
    var value: Double
    var startDate: Date
}
