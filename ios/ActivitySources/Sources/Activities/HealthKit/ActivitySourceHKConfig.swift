import Foundation
import HealthKit

public enum HealthKitConfigType {
    case activeEnergyBurned, stepCount, distanceCycling, distanceWalkingRunning, heartRate, workoutType
}

public struct ActivitySourceHKConfig {
    private let dataTypesToRead: [HealthKitConfigType]
    let syncUserEnteredData: Bool

    public init(dataTypesToRead: [HealthKitConfigType] = [.activeEnergyBurned, .stepCount, .distanceCycling, .distanceWalkingRunning, .heartRate, .workoutType],
                syncUserEnteredData: Bool = true) {

        self.dataTypesToRead = dataTypesToRead
        self.syncUserEnteredData = syncUserEnteredData
    }

    /// Types of data  Fjull SDK consumer wishes to read from HealthKit based on SDK config.
    /// - returns: A set of HKObjectType that wishes by SDK consumer and available for device.
    var typesToRead: Set<HKSampleType> {
        var dataTypes: Set<HKSampleType> = []

        if dataTypesToRead.contains(.activeEnergyBurned), let activeEnergyBurned = HKObjectType.quantityType(forIdentifier: .activeEnergyBurned) {
            dataTypes.insert(activeEnergyBurned)
        }

        if dataTypesToRead.contains(.distanceCycling), let distanceCycling = HKObjectType.quantityType(forIdentifier: .distanceCycling) {
            dataTypes.insert(distanceCycling)
        }

        if dataTypesToRead.contains(.stepCount), let stepCount = HKObjectType.quantityType(forIdentifier: .stepCount) {
            dataTypes.insert(stepCount)
        }

        if dataTypesToRead.contains(.distanceWalkingRunning), let distanceWalkingRunning = HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning) {
            dataTypes.insert(distanceWalkingRunning)
        }

        if dataTypesToRead.contains(.heartRate), let heartRate = HKObjectType.quantityType(forIdentifier: .heartRate) {
            dataTypes.insert(heartRate)
        }

        if dataTypesToRead.contains(.workoutType) {
            dataTypes.insert(HKObjectType.workoutType())
        }

        return dataTypes
    }
}
