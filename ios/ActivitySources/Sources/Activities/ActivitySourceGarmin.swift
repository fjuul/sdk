import Foundation
import FjuulCore
import Alamofire

final class ActivitySourceGarmin: ActivitySourceProtocol {
    static public let shared = ActivitySourceGarmin()

    var apiClient: ApiClient?
    var persistor: Persistor?
    
    private init() {}

    func mount(apiClient: ApiClient, persistor: Persistor) -> Bool {
        self.apiClient = apiClient
        self.persistor = persistor

        return true
    }

    func unmount() -> Bool {
        return true
    }
}
