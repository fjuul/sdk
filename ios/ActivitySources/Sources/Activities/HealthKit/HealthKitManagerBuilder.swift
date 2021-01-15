import Foundation
import FjuulCore

//sourcery: AutoMockable
protocol HealthKitManagerBuilder {
    init(persistor: Persistor, config: ActivitySourceConfigBuilder)
    func create(dataHandler: @escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)) -> HealthKitManaging
}

class HealthKitManagerConfigurator: HealthKitManagerBuilder {
    private var persistor: Persistor
    private var config: ActivitySourceConfigBuilder

    required init(persistor: Persistor, config: ActivitySourceConfigBuilder) {
        self.persistor = persistor
        self.config = config
    }
    
    func create(dataHandler: @escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)) -> HealthKitManaging {
        return HealthKitManager(persistor: self.persistor, config: self.config, dataHandler: dataHandler)
    }
}
