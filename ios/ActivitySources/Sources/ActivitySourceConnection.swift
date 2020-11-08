import Foundation
import FjuulCore

final class ActivitySourceConnection {
    private var trackerConnection: TrackerConnection
    public var activitySource: ActivitySourceProtocol

    init(trackerConnection: TrackerConnection, activitySource: ActivitySourceProtocol) {
        self.trackerConnection = trackerConnection
        self.activitySource = activitySource
    }

    var id: String {
        return trackerConnection.id
    }

    var tracker: String {
        return trackerConnection.tracker
    }

    var createdAt: Date {
        return trackerConnection.createdAt
    }

    var endedAt: Date? {
        return trackerConnection.endedAt
    }

    public func mount(apiClient: ApiClient, persistor: Persistor) -> Bool {
        activitySource.mount(apiClient: apiClient, persistor: persistor)
    }

    public func connected() -> Bool {
        guard let endedAt = trackerConnection.endedAt else { return true }

        return false
    }
}

enum ActivitySourceConnectionFactory {
    static func activitySourceConnection(trackerConnection: TrackerConnection) -> ActivitySourceConnection? {
        switch trackerConnection.tracker {
        case "healthkit":
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: ActivitySourceHK.shared)
        default:
            return nil
        }
    }
}
