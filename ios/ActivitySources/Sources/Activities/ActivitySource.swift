import Foundation
import FjuulCore

protocol ActivitySourceProtocol {
    var trackerConnection: TrackerConnection { get }
    var apiClient: ApiClient? { get }

    func mount(apiClient: ApiClient, persistor: Persistor) -> Bool
    func unmount() -> Bool
}

enum ActivitySourceFactory {
    static func activitySource(trackerConnection: TrackerConnection) -> ActivitySourceProtocol? {
        switch trackerConnection.tracker {
        case "healthkit":
            return ActivitySourceHK(trackerConnection: trackerConnection)
        default:
            return nil
        }
    }
}
