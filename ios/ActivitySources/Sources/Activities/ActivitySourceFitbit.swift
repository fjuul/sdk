import Foundation
import FjuulCore
import Alamofire

final class ActivitySourceFitbit: ActivitySourceProtocol {
    var trackerConnection: TrackerConnection
    var apiClient: ApiClient?
    var persistor: Persistor?

    init(trackerConnection: TrackerConnection) {
        self.trackerConnection = trackerConnection
    }

    func mount(apiClient: ApiClient, persistor: Persistor) -> Bool {
        self.apiClient = apiClient
        self.persistor = persistor

        return true
    }

    func unmount() -> Bool {
        return true
    }
}
