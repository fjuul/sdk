import Foundation
import FjuulCore
import Alamofire

public final class ActivitySourceFitbit: ActivitySource {
    static public let shared = ActivitySourceFitbit()

    public var tracker = ActivitySourcesItem.fitbit
    public var apiClient: ApiClient?
    public var persistor: Persistor?

    private init() {}

    public func mount(apiClient: ApiClient, persistor: Persistor, completion: (Result<Bool, Error>) -> Void) {
        self.apiClient = apiClient
        self.persistor = persistor

        completion(.success(true))
    }

    public func unmount(completion: @escaping (Result<Bool, Error>) -> Void) {
        completion(.success(true))
    }
}