import Foundation
import HealthKit

enum HKAnchorKey {
    case activeEnergyBurned
    case stepCount
    case distanceCycling
    case distanceWalkingRunning
    case heartRate
    case restingHeartRate
    case workout
    case bodyMass
    case height
}

/// DataStructure for save and retrieve in persisted store HealthKit anchors
public struct HKAnchorData: Codable, Equatable {
    private var activeEnergyBurnedRaw: Data?
    private var stepCountRaw: Data?
    private var distanceCyclingRaw: Data?
    private var distanceWalkingRunningRaw: Data?
    private var heartRateRaw: Data?
    private var restingHeartRateRaw: Data?
    private var workoutRaw: Data?
    private var bodyMassRaw: Data?
    private var heightRaw: Data?

    subscript(key: HKAnchorKey) -> HKQueryAnchor? {
        get {
            var valueRaw: Data?

            switch key {
            case .activeEnergyBurned:
                valueRaw = activeEnergyBurnedRaw
            case .stepCount:
                valueRaw = stepCountRaw
            case .distanceCycling:
                valueRaw = distanceCyclingRaw
            case .distanceWalkingRunning:
                valueRaw = distanceWalkingRunningRaw
            case .heartRate:
                valueRaw = heartRateRaw
            case .restingHeartRate:
                valueRaw = restingHeartRateRaw
            case .workout:
                valueRaw = workoutRaw
            case .bodyMass:
                valueRaw = bodyMassRaw
            case .height:
                valueRaw = heightRaw
            }

            if let value = valueRaw {
                do {
                    let decoded = try NSKeyedUnarchiver.unarchivedObject(ofClasses: [NSNull.self, HKQueryAnchor.self], from: value)
                    if decoded is NSNull {
                        return nil
                    }
                    return decoded as? HKQueryAnchor
                } catch {
                    DataLogger.shared.error("Error while getting anchor data: \(error)")
                    return nil
                }
            } else {
                return nil
            }
        }
        set(newValue) {
            do {
                let newValueRaw = try NSKeyedArchiver.archivedData(withRootObject: newValue as Any, requiringSecureCoding: false)

                switch key {
                case .activeEnergyBurned:
                    activeEnergyBurnedRaw = newValueRaw
                case .stepCount:
                    stepCountRaw = newValueRaw
                case .distanceCycling:
                    distanceCyclingRaw = newValueRaw
                case .distanceWalkingRunning:
                    distanceWalkingRunningRaw = newValueRaw
                case .heartRate:
                    heartRateRaw = newValueRaw
                case .restingHeartRate:
                    restingHeartRateRaw = newValueRaw
                case .workout:
                    workoutRaw = newValueRaw
                case .bodyMass:
                    bodyMassRaw = newValueRaw
                case .height:
                    heightRaw = newValueRaw
                }
            } catch {
                DataLogger.shared.error("Error while setting anchor data: \(error)")
            }
        }
    }
}
