import Foundation
import FjuulCore

/// A tracker connection bound with ActivitySource
public final class ActivitySourceConnection: TrackerConnectionable, Equatable {
    public static func == (lhs: ActivitySourceConnection, rhs: ActivitySourceConnection) -> Bool {
        return lhs.id == rhs.id
    }

    public let id: String
    public let tracker: TrackerValue
    public let createdAt: Date
    public let endedAt: Date?

    public var activitySource: ActivitySource

    init(trackerConnection: TrackerConnection, activitySource: ActivitySource) {
        id = trackerConnection.id
        tracker = TrackerValue(value: trackerConnection.tracker)
        createdAt = trackerConnection.createdAt
        endedAt = trackerConnection.endedAt

        self.activitySource = activitySource
    }

    /// Mount locally ActivitySource if it is mountable (protocol: MountableActivitySource).
    /// Currently on HealthKitActivitySource is mountable, and call this function will setup backgroundDelivery.
    /// - Parameters:
    ///   - apiClient: instance of ActivitySourcesApiClient
    ///   - config: instance of ActivitySourceConfigBuilder
    ///   - persistor: instance of Persistor
    ///   - completion: void or error
    func mount(apiClient: ActivitySourcesApiClient, config: ActivitySourceConfigBuilder, persistor: Persistor, completion: @escaping (Result<Void, Error>) -> Void) {
        if let mountableSource = activitySource as? MountableActivitySource {
            config.healthKitConfig.syncDataFrom = self.createdAt

            let healthKitManagerBuilder = HealthKitManagerBuilder(apiClient: apiClient, persistor: persistor, config: config)

            mountableSource.mount(apiClient: apiClient, config: config, healthKitManagerBuilder: healthKitManagerBuilder) { result in
                completion(result)
            }
        } else {
            completion(.success(()))
        }
    }

    /// Unmount locally ActivitySource if it is mountable (protocol: MountableActivitySource).
    /// Currently on HealthKitActivitySource is mountable, and call this function will disable backgroundDelivery.
    /// - Parameter completion: void or error
    func unmount(completion: @escaping (Result<Void, Error>) -> Void) {
        if let mountableSource = activitySource as? MountableActivitySource {
            mountableSource.unmount { result in
                completion(result)
            }
        } else {
            completion(.success(()))
        }
    }
}

enum ActivitySourceConnectionFactory {
    static func activitySourceConnection(trackerConnection: TrackerConnection) -> ActivitySourceConnection {
        let trackerValue = TrackerValue.forValue(value: trackerConnection.tracker)

        if TrackerValue.HEALTHKIT == trackerValue {
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: HealthKitActivitySource.shared)
        } else if TrackerValue.POLAR == trackerValue {
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: PolarActivitySource.shared)
        } else if TrackerValue.GARMIN == trackerValue {
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: GarminActivitySource.shared)
        } else if TrackerValue.STRAVA == trackerValue {
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: StravaActivitySource.shared)
        } else if TrackerValue.SUUNTO == trackerValue {
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: SuuntoActivitySource.shared)
        } else if TrackerValue.FITBIT == trackerValue {
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: FitbitActivitySource.shared)
        } else if TrackerValue.WITHINGS == trackerValue {
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: WithingsActivitySource.shared)
        } else {
            return ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: UnknownActivitySource(tracker: trackerConnection.tracker))
        }
    }
}
