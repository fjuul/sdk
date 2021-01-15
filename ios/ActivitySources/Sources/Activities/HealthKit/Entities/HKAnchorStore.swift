import Foundation
import FjuulCore
import HealthKit

public struct HKAnchorStore {

    private var persistor: Persistor
    private let lookupKey: String

    init(userToken: String, persistor: Persistor) {
        self.persistor = persistor
        self.lookupKey = "healthkit-anchor.\(userToken)"
    }

    var anchors: HKAnchorData? {
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
