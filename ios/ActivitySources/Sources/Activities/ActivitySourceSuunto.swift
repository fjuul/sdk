import Foundation
import FjuulCore
import Alamofire

public final class ActivitySourceSuunto: ActivitySource {
    static public let shared = ActivitySourceSuunto()

    public var tracker = ActivitySourcesItem.suunto
    public var apiClient: ApiClient?
    public var persistor: Persistor?

    private init() {}

    public func mount(apiClient: ApiClient, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void) {
        self.apiClient = apiClient
        self.persistor = persistor

        completion(.success(true))
    }

    public func unmount(completion: @escaping (Result<Bool, Error>) -> Void) {
        completion(.success(true))
    }
}