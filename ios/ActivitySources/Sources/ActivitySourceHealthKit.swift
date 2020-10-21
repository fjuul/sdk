#if canImport(HealthKit)
import Foundation
import FjuulCore

class ActivitySourceHealthKit {
    internal let hkDataManager: HKDataManager = HKDataManager(persistor: DiskPersistor())

    init() {}

    // TODO: Check with team, but probably that the best place for set activitySource on server side
    func mount() {
        hkDataManager.authorizeHealthKitAccess { (success, error) in
            if success {
                self.hkDataManager.observe()
            } else {
                if error != nil {
                    // TODO: return better error
                    print("\(String(describing: error))")
                }
            }
        }
    }

    func unmount() {
    }
    func sync() {
        // TODO: sync types based on config
//        self.fetchIntradayUpdates(sampleType: HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!)
    }
    func mountBackgroundDelivery() {}
    func unmountBackgroundDelivery() {}
}
#endif
