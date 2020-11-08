import Foundation
import FjuulCore

final class ActivitySourcePolar: ActivitySourceProtocol {
    static public let shared = ActivitySourcePolar()

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
