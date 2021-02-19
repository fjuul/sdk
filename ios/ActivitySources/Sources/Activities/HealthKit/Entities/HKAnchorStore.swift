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
    mutating func save(type: HKObjectType, newAnchor: HKQueryAnchor?) throws {
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
        case HKObjectType.quantityType(forIdentifier: .bodyMass):
            anchors?[.bodyMass] = newAnchor
        case HKObjectType.quantityType(forIdentifier: .height):
            anchors?[.height] = newAnchor
        default:
            throw FjuulError.activitySourceFailure(reason: .wrongHealthKitObjectType)
        }
    }

    /// Fetch anchor from persisted store based on HKObjectType
    func get(type: HKObjectType) throws -> HKQueryAnchor? {
        switch type {
        case HKObjectType.quantityType(forIdentifier: .activeEnergyBurned):
            return anchors?[.activeEnergyBurned]
        case HKObjectType.quantityType(forIdentifier: .stepCount):
            return anchors?[.stepCount]
        case HKObjectType.quantityType(forIdentifier: .distanceCycling):
            return anchors?[.distanceCycling]
        case HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning):
            return anchors?[.distanceWalkingRunning]
        case HKObjectType.quantityType(forIdentifier: .heartRate):
            return anchors?[.heartRate]
        case HKObjectType.workoutType():
            return anchors?[.workout]
        case HKObjectType.quantityType(forIdentifier: .bodyMass):
            return anchors?[.bodyMass]
        case HKObjectType.quantityType(forIdentifier: .height):
            return anchors?[.height]
        default:
            throw FjuulError.activitySourceFailure(reason: .wrongHealthKitObjectType)
        }
    }
}
