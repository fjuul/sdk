import Foundation
import FjuulCore

public final class ActivitySourceConnection: TrackerConnectionable {
    public let id: String
    public let tracker: ActivitySourcesItem?
    public let createdAt: Date
    public let endedAt: Date?

    public var activitySource: ActivitySource

    init(trackerConnection: TrackerConnection, activitySource: ActivitySource) {
        id = trackerConnection.id
        tracker = ActivitySourcesItem(rawValue: trackerConnection.tracker)
        createdAt = trackerConnection.createdAt
        endedAt = trackerConnection.endedAt

        self.activitySource = activitySource
    }

    func mount(apiClient: ActivitySourcesApi, config: ActivitySourceConfigBuilder, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void) {
        if let mountableSource = activitySource as? MountableActivitySource {
            mountableSource.mount(apiClient: apiClient, config: config, persistor: persistor) { result in
                completion(result)
            }
        } else {
            completion(.success(true))
        }
    }

    func unmount(completion: @escaping (Result<Bool, Error>) -> Void) {
        if let mountableSource = activitySource as? MountableActivitySource {
            mountableSource.unmount { result in
                completion(result)
            }
        } else {
            completion(.success(true))
        }
    }

    public func connected() -> Bool {
        guard endedAt != nil else { return false }

        return false
    }
}

enum ActivitySourceConnectionFactory {
    static func activitySourceConnection(trackerConnection: TrackerConnection) -> ActivitySourceConnection? {
        guard let tracker = ActivitySourcesItem(rawValue: trackerConnection.tracker) else { return nil }

        switch tracker {
        case ActivitySourcesItem.healthkit:
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: ActivitySourceHK.shared)
        case ActivitySourcesItem.polar:
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: ActivitySourcePolar.shared)
        case ActivitySourcesItem.garmin:
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: ActivitySourceGarmin.shared)
        case ActivitySourcesItem.suunto:
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: ActivitySourceSuunto.shared)
        case ActivitySourcesItem.fitbit:
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: ActivitySourceFitbit.shared)
        default:
            return nil
        }
    }
}
