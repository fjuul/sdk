import Foundation
import HealthKit

enum HKAnchorKey {
    case activeEnergyBurned
    case stepCount
    case distanceCycling
    case distanceWalkingRunning
    case heartRate
    case workout
}

public struct HKAnchorData: Codable, Equatable {
    private var activeEnergyBurnedRaw: Data?
    private var stepCountRaw: Data?
    private var distanceCyclingRaw: Data?
    private var distanceWalkingRunningRaw: Data?
    private var heartRateRaw: Data?
    
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
            default:
                return nil
            }

            if let value = valueRaw {
                return NSKeyedUnarchiver.unarchiveObject(with: value) as? HKQueryAnchor
            } else {
                return nil
            }
        }
        set(newValue) {
            let newValueRaw = NSKeyedArchiver.archivedData(withRootObject: newValue as Any)

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
            default:
                print("Not found key for save data")
            }
        }
    }
}
