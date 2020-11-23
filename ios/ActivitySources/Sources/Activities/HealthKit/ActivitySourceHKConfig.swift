import Foundation

public enum HealthKitConfigType {
    case activeEnergyBurned, stepCount, distanceCycling, distanceWalkingRunning, heartRate, workoutType
}

public struct ActivitySourceHKConfig {
    let dataTypesToRead: [HealthKitConfigType]
    let syncUserEnteredData: Bool

    public init(dataTypesToRead: [HealthKitConfigType], syncUserEnteredData: Bool = true) {
        self.dataTypesToRead = dataTypesToRead
        self.syncUserEnteredData = syncUserEnteredData
    }
}
