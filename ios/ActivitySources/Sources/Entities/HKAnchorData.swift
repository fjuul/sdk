import Foundation
import HealthKit

public struct HKAnchorData: Codable, Equatable {
    var activeEnergyBurnedRaw: Data?
    var stepCount: Data?
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

}
