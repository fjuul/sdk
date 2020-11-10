import Foundation
import FjuulCore
import Alamofire

final class ActivitySourceGarmin: ActivitySource {
    static public let shared = ActivitySourceGarmin()

    var apiClient: ApiClient?
    var persistor: Persistor?
    
    private init() {}

    func mount(apiClient: ApiClient, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void) {
        self.apiClient = apiClient
        self.persistor = persistor

        completion(.success(true))
    }

    func unmount(completion: @escaping (Result<Bool, Error>) -> Void) {
        completion(.success(true))
    }
}
