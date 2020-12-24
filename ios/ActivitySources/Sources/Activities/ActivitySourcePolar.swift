import Foundation
import FjuulCore

public final class ActivitySourcePolar: ActivitySource {
    static public let shared = ActivitySourcePolar()

    public var tracker = ActivitySourcesItem.polar
//    public var apiClient: ActivitySourcesApi?
    public var persistor: Persistor?

    private init() {}

    public func mount(apiClient: ActivitySourcesApi, config: ActivitySourceConfigBuilder, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void) {
//        self.apiClient = apiClient
        self.persistor = persistor

        completion(.success(true))
    }

    public func unmount(completion: @escaping (Result<Bool, Error>) -> Void) {
//        self.apiClient = nil
//        self.persistor = nil
        completion(.success(true))
    }
}
