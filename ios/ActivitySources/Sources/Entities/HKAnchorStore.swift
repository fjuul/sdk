import Foundation
import FjuulCore
import HealthKit

public struct HKAnchorStore {

    private var persistor: Persistor
    private let lookupKey = "hk-anchor-key-v2"

    init(persistor: Persistor) {
        self.persistor = persistor
    }

    var anchor: HKAnchorData? {
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

}
