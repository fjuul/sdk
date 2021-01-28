import Foundation
import FjuulCore
import HealthKit

/// Persisted store for save Healthkit anchors
struct HKAnchorStore {

    private var persistor: Persistor
    private let lookupKey: String

    init(userToken: String, persistor: Persistor) {
        self.persistor = persistor
        self.lookupKey = "healthkit-anchor.\(userToken)"
    }

    private var anchors: HKAnchorData? {
        get {
            if let value = persistor.get(key: lookupKey) as HKAnchorData? {
               return value
            } else {
                return HKAnchorData()
            }
        }
        set {
            persistor.set(key: lookupKey, value: newValue)
        }
    }

    /// Saves anchor in persisted store based on HKObjectType
    mutating func save(type: HKObjectType, newAnchor: HKQueryAnchor?) {
        switch type {
        case HKObjectType.quantityType(forIdentifier: .activeEnergyBurned):
            anchors?[.activeEnergyBurned] = newAnchor
        case HKObjectType.quantityType(forIdentifier: .stepCount):
            anchors?[.stepCount] = newAnchor
        case HKObjectType.quantityType(forIdentifier: .distanceCycling):
            anchors?[.distanceCycling] = newAnchor
        case HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning):
            anchors?[.distanceWalkingRunning] = newAnchor
        case HKObjectType.quantityType(forIdentifier: .heartRate):
            anchors?[.heartRate] = newAnchor
        case HKObjectType.workoutType():
            anchors?[.workout] = newAnchor
        default:
            return
        }
    }

    /// Fetch anchor from persisted store based on HKObjectType
    func get(type: HKObjectType) -> HKQueryAnchor {
        let defaultValue = HKQueryAnchor.init(fromValue: 0)

        switch type {
        case HKObjectType.quantityType(forIdentifier: .activeEnergyBurned):
            return anchors?[.activeEnergyBurned] ?? defaultValue
        case HKObjectType.quantityType(forIdentifier: .stepCount):
            return anchors?[.stepCount] ?? defaultValue
        case HKObjectType.quantityType(forIdentifier: .distanceCycling):
            return anchors?[.distanceCycling] ?? defaultValue
        case HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning):
            return anchors?[.distanceWalkingRunning] ?? defaultValue
        case HKObjectType.quantityType(forIdentifier: .heartRate):
            return anchors?[.heartRate] ?? defaultValue
        case HKObjectType.workoutType():
            return anchors?[.workout] ?? defaultValue
        default:
            return defaultValue
        }
    }
}
