import Foundation
import HealthKit

public struct HKAnchorData: Codable, Equatable {
    private var activeEnergyBurnedRaw: Data?
    private var stepCountRaw: Data?
    private var distanceCyclingRaw: Data?
    private var distanceWalkingRunningRaw: Data?
    private var heartRateRaw: Data?

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
    
    var heartRate: HKQueryAnchor? {
        get {
            if let value = heartRateRaw {
                return NSKeyedUnarchiver.unarchiveObject(with: value) as? HKQueryAnchor
            } else {
                return nil
            }
        }
        set {
            heartRateRaw = NSKeyedArchiver.archivedData(withRootObject: newValue as Any)
        }
    }
}

// TODO: Add extension with func for fetch and setup HKQueryAnchor, for avoid duplications
