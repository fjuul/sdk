import Foundation
import HealthKit

/// Data types that availbale configure for HealthKitActivitySourceConfig
public enum HealthKitConfigType: Int, CaseIterable {
    case activeEnergyBurned, stepCount, distanceCycling, distanceWalkingRunning, heartRate, workout, weight, height

    var sampleType: HKSampleType? {
        switch self {
        case .activeEnergyBurned:       return HKObjectType.quantityType(forIdentifier: .activeEnergyBurned)
        case .distanceCycling:          return HKObjectType.quantityType(forIdentifier: .distanceCycling)
        case .distanceWalkingRunning:   return HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning)
        case .heartRate:                return HKObjectType.quantityType(forIdentifier: .heartRate)
        case .height:                   return HKObjectType.quantityType(forIdentifier: .height)
        case .stepCount:                return HKObjectType.quantityType(forIdentifier: .stepCount)
        case .weight:                   return HKObjectType.quantityType(forIdentifier: .bodyMass)
        case .workout:                  return HKObjectType.workoutType()
        }
    }
}

extension HealthKitConfigType {
    public static let intradayTypes: [HealthKitConfigType] = [.activeEnergyBurned, .distanceCycling, .distanceWalkingRunning, .heartRate, .stepCount]
    public static let userProfileTypes: [HealthKitConfigType] = [.height, .weight]
}

/// Config for the HealthKit ActivitySource. SDK consumers can configure which data types SDK will collect.
public struct HealthKitActivitySourceConfig {
    private let dataTypesToRead: [HealthKitConfigType]
    let syncUserEnteredData: Bool
    internal var syncDataFrom: Date?

    public init(dataTypesToRead: [HealthKitConfigType] = [.activeEnergyBurned, .stepCount, .distanceCycling, .distanceWalkingRunning, .heartRate, .workout, .weight, .height],
                syncUserEnteredData: Bool = true) {

        self.dataTypesToRead = dataTypesToRead
        self.syncUserEnteredData = syncUserEnteredData
    }

    /// Types of HealthKit data that Fjull SDK consumer wishes to read from HealthKit based on SDK config.
    /// - returns: A set of HKObjectType that wishes by SDK consumer and available for the device.
    var typesToRead: Set<HKSampleType> {
        return Set(dataTypesToRead.compactMap { item in item.sampleType })
    }
}
