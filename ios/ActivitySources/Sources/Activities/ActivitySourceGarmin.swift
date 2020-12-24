import Foundation
import FjuulCore
import Alamofire

public final class ActivitySourceGarmin: ActivitySource {
    static public let shared = ActivitySourceGarmin()

    public var tracker = ActivitySourcesItem.garmin
//    public var apiClient: ActivitySourcesApi?
    public var persistor: Persistor?

    private init() {}

    public func mount(apiClient: ActivitySourcesApi, config: ActivitySourceConfigBuilder, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void) {
//        self.apiClient = apiClient
        self.persistor = persistor

        completion(.success(true))
    }

    public func unmount(completion: @escaping (Result<Bool, Error>) -> Void) {
        completion(.success(true))
    }
}
