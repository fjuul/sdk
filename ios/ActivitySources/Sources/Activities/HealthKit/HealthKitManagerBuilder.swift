import Foundation
import FjuulCore

class HealthKitManagerBuilder {
    private var persistor: Persistor
    private var config: ActivitySourceConfigBuilder

    init(persistor: Persistor, config: ActivitySourceConfigBuilder) {
        self.persistor = persistor
        self.config = config
    }
    
    func create(dataHandler: @escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)) -> HealthKitManager {
        return HealthKitManager(persistor: self.persistor, config: self.config, dataHandler: dataHandler)
    }
}
