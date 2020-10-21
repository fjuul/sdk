import Foundation
import FjuulCore
import HealthKit

public struct HKAnchorStore {
    private var persistor: Persistor

    init(persistor: Persistor) {
        self.persistor = persistor
    }

    public var description: String {
        return "Anchor: "
    }

    var anchor: HKQueryAnchor? {
        get {
            if let value = persistor.get(key: "valuev5") {
                NSKeyedUnarchiver.unarchiveObject(with: value) as? HKQueryAnchor
            } else {
                return nil
            }
        }
        set {
            if let newAnchor = newValue {
                let data: Data = NSKeyedArchiver.archivedData(withRootObject: newAnchor as Any)
                persistor.set(key: "valuev5", value: data)
            }
        }
    }
}
