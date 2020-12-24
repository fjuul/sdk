import Foundation
//import FjuulCore

// Structure for aggregated values of
// - activeEnergyBurned with unit kilocalorie
// - stepCount with unit count
// - distanceCycling
// - distanceWalkingRunning
// Data aggregated by 1m granulariry

struct AggregatedDataPoint: Codable {
    var value: Double
    var start: Date
}
