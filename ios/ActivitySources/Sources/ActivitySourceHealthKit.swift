#if canImport(HealthKit)
import Foundation
import FjuulCore

class ActivitySourceHealthKit {
    let hkDataManager: HKDataManager
    let persistor: Persistor
    let apiClient: ApiClient

    init(apiClient: ApiClient, persistor: Persistor = DiskPersistor()) {
        self.apiClient = apiClient
        self.persistor = persistor
        self.hkDataManager = HKDataManager(apiClient: apiClient, persistor: persistor)
    }

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
    // TODO: Check are we need allow SDK consumer sync particular date?
    func sync() {
        // TODO: sync types based on config
//        self.fetchIntradayUpdates(sampleType: HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!)
    }
    func mountBackgroundDelivery() {}
    func unmountBackgroundDelivery() {}
}
#endif
