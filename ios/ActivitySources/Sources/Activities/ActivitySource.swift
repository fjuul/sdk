import Foundation
import FjuulCore

public protocol ActivitySource {
    var tracker: ActivitySourcesItem { get }
}

protocol MountableActivitySource: ActivitySource {
    var apiClient: ActivitySourcesApiClient? { get }

    func mount(apiClient: ActivitySourcesApiClient, config: ActivitySourceConfigBuilder, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void)
    func unmount(completion: @escaping (Result<Bool, Error>) -> Void)
}
