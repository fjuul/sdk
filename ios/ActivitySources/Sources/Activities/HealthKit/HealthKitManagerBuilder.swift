import Foundation
import FjuulCore

//sourcery: AutoMockable
protocol HealthKitManagerBuilding {
    init(apiClient: ActivitySourcesApiClient, persistor: Persistor, config: ActivitySourceConfigBuilder)
    func create(dataHandler: @escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)) -> HealthKitManaging
}

/// Manager for configure and build HealthKitManager
class HealthKitManagerBuilder: HealthKitManagerBuilding {
    private let persistor: Persistor
    private let config: ActivitySourceConfigBuilder
    private let apiClient: ActivitySourcesApiClient

    required init(apiClient: ActivitySourcesApiClient, persistor: Persistor, config: ActivitySourceConfigBuilder) {
        self.persistor = persistor
        self.config = config
        self.apiClient = apiClient
    }

    func create(dataHandler: @escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)) -> HealthKitManaging {
        let anchorStore = HKAnchorStore(userToken: apiClient.apiClient.userToken, persistor: persistor)

        return HealthKitManager(anchorStore: anchorStore, config: self.config, dataHandler: dataHandler)
    }
}
