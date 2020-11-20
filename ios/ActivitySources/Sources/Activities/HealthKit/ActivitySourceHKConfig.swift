import Foundation

public enum HealthKitConfigType {
    case activeEnergyBurned, stepCount, distanceCycling, distanceWalkingRunning, heartRate, workoutType
}

public struct ActivitySourceHKConfig {
    var dataTypesToRead: [HealthKitConfigType]

    public init(dataTypesToRead: [HealthKitConfigType]) {
        self.dataTypesToRead = dataTypesToRead
    }
}
