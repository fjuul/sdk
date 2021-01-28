import Foundation
import FjuulCore

/// A tracker connection bound with ActivitySource
public final class ActivitySourceConnection: TrackerConnectionable, Equatable {
    public static func == (lhs: ActivitySourceConnection, rhs: ActivitySourceConnection) -> Bool {
        return lhs.id == rhs.id
    }

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

    /// Mount locally ActivitySource if it is mountable (protocol: MountableActivitySource).
    /// Currently on ActivitySourceHK is mountable, and call this function will setup backgroundDelivery.
    /// - Parameters:
    ///   - apiClient: instance of ActivitySourcesApiClient
    ///   - config: instance of ActivitySourceConfigBuilder
    ///   - persistor: instance of Persistor
    ///   - completion: status or error
    func mount(apiClient: ActivitySourcesApiClient, config: ActivitySourceConfigBuilder, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void) {
        if let mountableSource = activitySource as? MountableActivitySource {
            let healthKitManagerBuilder = HealthKitManagerBuilder(apiClient: apiClient, persistor: persistor, config: config)

            mountableSource.mount(apiClient: apiClient, config: config, healthKitManagerBuilder: healthKitManagerBuilder) { result in
                completion(result)
            }
        } else {
            completion(.success(true))
        }
    }

    /// Unmount locally ActivitySource if it is mountable (protocol: MountableActivitySource).
    /// Currently on ActivitySourceHK is mountable, and call this function will disable backgroundDelivery.
    /// - Parameter completion: status or error
    func unmount(completion: @escaping (Result<Bool, Error>) -> Void) {
        if let mountableSource = activitySource as? MountableActivitySource {
            mountableSource.unmount { result in
                completion(result)
            }
        } else {
            completion(.success(true))
        }
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
