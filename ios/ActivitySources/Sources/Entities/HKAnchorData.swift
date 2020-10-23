import Foundation
import HealthKit

public struct HKAnchorData: Codable, Equatable {
    var activeEnergyBurnedRaw: Data?
    var stepCountRaw: Data?
    var distanceCycling: Data?
    var distanceWalkingRunning: Data?
    var heartRate: Data?

    var activeEnergyBurned: HKQueryAnchor? {
        get {
            if let value = activeEnergyBurnedRaw {
                return NSKeyedUnarchiver.unarchiveObject(with: value) as? HKQueryAnchor
            } else {
                return nil
            }
        }
        set {
            activeEnergyBurnedRaw = NSKeyedArchiver.archivedData(withRootObject: newValue as Any)
        }
    }

    var stepCount: HKQueryAnchor? {
        get {
            if let value = stepCountRaw {
                return NSKeyedUnarchiver.unarchiveObject(with: value) as? HKQueryAnchor
            } else {
                return nil
            }
        }
        set {
            stepCountRaw = NSKeyedArchiver.archivedData(withRootObject: newValue as Any)
        }
    }
}

// TODO: Add extension with func for fetch and setup HKQueryAnchor, for avoid duplications
