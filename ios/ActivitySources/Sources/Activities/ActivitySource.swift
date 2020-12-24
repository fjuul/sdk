import Foundation
import FjuulCore

public protocol ActivitySource {
    var tracker: ActivitySourcesItem { get }
}

protocol MountableActivitySource: ActivitySource {
    var apiClient: ActivitySourcesApi? { get }

    func mount(apiClient: ActivitySourcesApi, config: ActivitySourceConfigBuilder, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void)
    func unmount(completion: @escaping (Result<Bool, Error>) -> Void)
}
