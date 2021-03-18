import Foundation
import HealthKit

/// Data types that availbale configure for HealthKitActivitySourceConfig
public enum HealthKitConfigType: Int, CaseIterable {
    case activeEnergyBurned, stepCount, distanceCycling, distanceWalkingRunning, heartRate, workout, weight, height
    
    /// Appropriate type of HKSampleType
    public var sampleType: HKSampleType? {
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
    let forcedLowerDateBoundaryForHealthKit: Date?
    let enableBackgroundDelivery: Bool
    internal var syncDataFrom: Date?

    /// Config for HealthKit data sync.
    /// - Parameters:
    ///   - dataTypesToRead: List of data types for read from HealthKit.
    ///   - syncUserEnteredData: Enable/Disable sync from HealthKit user manual intered data.
    ///   - forcedLowerDateBoundaryForHealthKit: Sets the forced lower date boundary from which syncing will work from. It overrides the default limit which is dictated by the
    ///   creation date of the connection to HealthKit. This method was added only for internal purposes. Do not use it!
    ///   - enableBackgroundDelivery: Enable/Disable backgroundDelivery. By default it's enabled.
    public init(dataTypesToRead: [HealthKitConfigType] = HealthKitConfigType.allCases,
                syncUserEnteredData: Bool = true,
                forcedLowerDateBoundaryForHealthKit: Date? = nil,
                enableBackgroundDelivery: Bool = true) {

        self.dataTypesToRead = dataTypesToRead
        self.syncUserEnteredData = syncUserEnteredData
        self.forcedLowerDateBoundaryForHealthKit = forcedLowerDateBoundaryForHealthKit
        self.enableBackgroundDelivery = enableBackgroundDelivery
    }

    /// Types of HealthKit data that Fjull SDK consumer wishes to read from HealthKit based on SDK config.
    /// - returns: A set of HKObjectType that wishes by SDK consumer and available for the device.
    var typesToRead: Set<HKSampleType> {
        return Set(dataTypesToRead.compactMap { item in item.sampleType })
    }
}
