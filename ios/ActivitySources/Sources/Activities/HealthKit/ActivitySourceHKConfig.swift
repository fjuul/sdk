import Foundation
import HealthKit

/// Data types that availbale configure for ActivitySourceHKConfig
public enum HealthKitConfigType {
    case activeEnergyBurned, stepCount, distanceCycling, distanceWalkingRunning, heartRate, workoutType
}

/// Config for the HealthKit ActivitySource. SDK consumers can configure which data types SDK will collect.
public struct ActivitySourceHKConfig {
    private let dataTypesToRead: [HealthKitConfigType]
    let syncUserEnteredData: Bool

    public init(dataTypesToRead: [HealthKitConfigType] = [.activeEnergyBurned, .stepCount, .distanceCycling, .distanceWalkingRunning, .heartRate, .workoutType],
                syncUserEnteredData: Bool = true) {

        self.dataTypesToRead = dataTypesToRead
        self.syncUserEnteredData = syncUserEnteredData
    }

    /// Types of HealthKit data that Fjull SDK consumer wishes to read from HealthKit based on SDK config.
    /// - returns: A set of HKObjectType that wishes by SDK consumer and available for the device.
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
