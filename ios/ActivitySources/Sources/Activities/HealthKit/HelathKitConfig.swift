import Foundation

public enum HealthKitConfigType {
    case activeEnergyBurned, stepCount, distanceCycling, distanceWalkingRunning, heartRate, workoutType
}

public struct HealthKitConfig {
    var dataTypes: [HealthKitConfigType]

    public init(dataTypes: [HealthKitConfigType]) {
        self.dataTypes = dataTypes
    }
}
