import Foundation
import FjuulCore

final class ActivitySourceConnection {
    private var trackerConnection: TrackerConnection
    public var activitySource: ActivitySource

    init(trackerConnection: TrackerConnection, activitySource: ActivitySource) {
        self.trackerConnection = trackerConnection
        self.activitySource = activitySource
    }

    var id: String {
        return trackerConnection.id
    }

    var tracker: ActivitySourcesItem? {
        return ActivitySourcesItem(rawValue: trackerConnection.tracker)
    }

    var createdAt: Date {
        return trackerConnection.createdAt
    }

    var endedAt: Date? {
        return trackerConnection.endedAt
    }

    public func mount(apiClient: ApiClient, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void) {
        activitySource.mount(apiClient: apiClient, persistor: persistor) { result in
            completion(result)
        }
    }

    public func connected() -> Bool {
        guard trackerConnection.endedAt != nil else { return false }

        return false
    }
}

enum ActivitySourceConnectionFactory {
    static func activitySourceConnection(trackerConnection: TrackerConnection) -> ActivitySourceConnection? {
        guard let tracker = ActivitySourcesItem(rawValue: trackerConnection.tracker) else { return nil }

        switch tracker {
        case ActivitySourcesItem.healthkit:
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: ActivitySourceHK.shared)
        default:
            return nil
        }
    }
}
