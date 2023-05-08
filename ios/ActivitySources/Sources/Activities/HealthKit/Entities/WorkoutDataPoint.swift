import Foundation

struct WorkoutDataPoint: Codable {
    var uuid: String
    var sourceBundleIdentifier: String
    var startDate: Date
    var endDate: Date
    var totalDistance: Double // Unit meters
    var totalEnergyBurned: Double // Unit kilocalories
    var workoutActivityType: String
    var totalSwimmingStrokeCount: Double
    var workoutEvents: [WorkoutEventData]?
    var metadata: [String: String]?
    var heartrates: [WorkoutSampleData]?
    var walkingRunningDistances: [WorkoutSampleData]?
    var activeEnergyBurned: [WorkoutSampleData]?
    var cyclingDistances: [WorkoutSampleData]?
}

struct WorkoutEventData: Codable {
    var startDate: Date
    var type: String // Pause, resume, lap, segment, etc
    var metadata: [String: String]?
}

struct WorkoutSampleData: Codable {
  var value: Double // Value can be for heartrates, meters or activeEnergyBurned
  var startDate: Date
  var endDate: Date
}
