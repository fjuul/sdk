import Foundation
import FjuulCore
import Logging

// Logger with default label
private let logger = Logger(label: "FjuulSDK")

// FIXME: Add link on deep linking description
/**
 The ActivitySourcesManager encapsulates a connection to fitness trackers, access to current user's tracker connections.
 This is a high-level entity and entry point of the ActivitySources module. The class is designed as the singleton, so you need first to initialize it
 before getting the instance. For the proper initialization, you have to provide the configured API-client built with the user credentials.
  
 One of the main functions of this module is to connect to activity sources. There are local (i.e. HealthKit) and external trackers (i.e. Polar, Garmin, Fitbit, etc).
 External trackers require user authentication in the web browser.
 
 For handle authentication with external trackers, it require support deep linking in app.
*/
final public class ActivitySourceManager {
    var apiClient: ActivitySourcesApiClient
    var mountedActivitySourceConnections: [ActivitySourceConnection] = []
    var config: ActivitySourceConfigBuilder

    private var persistor: Persistor
    private var connectionsLocalStore: ActivitySourceStore

    /// Internal initializer
    /// - Parameters:
    ///   - userToken: User token
    ///   - persistor: for persisted data like HealthKit anchors
    ///   - apiClient: configured client of ActivitySourcesApiClient
    ///   - config: Activity source config
    internal init(userToken: String, persistor: Persistor, apiClient: ActivitySourcesApiClient, config: ActivitySourceConfigBuilder) {
        self.apiClient = apiClient
        self.persistor = persistor
        self.config = config
        self.connectionsLocalStore = ActivitySourceStore(userToken: userToken, persistor: persistor)

        self.restoreState { _ in
            self.refreshCurrent { _ in
                logger.info("Initial sync current connections")
            }
        }
    }

    /// Connect specified ActivitySource.
    /// After getting it in the callback, you need to do one of the following:
    ///  1) If connects to the Healthkit tracker, iOS will show a modal prompting with list of required Healthkit data permissions.
    ///  2) If connects to the any external trackers (Garmin, Polar, etc...), This will open the user's web browser on the page with authorization of the specified tracker.
    ///    After the user successfully authenticates, the user will be redirected back to the app by the link
    ///    matched with the scheme provided to you or coordinated with you by Fjuul.
    /// After a user succeeds in the connection, please invoke refreshing current connections `ActivitySourcesManager#getCurrentConnections`
    /// - Parameters:
    ///   - activitySource: ActivitySource instance to connect (ActivitySourcePolar.shared, HealthKitActivitySource.shared, etc...)
    ///   - completion: with ConnectionResult or Error
    public func connect(activitySource: ActivitySource, completion: @escaping (Result<ConnectionResult, Error>) -> Void) {
        if let activitySource = activitySource as? MountableHealthKitActivitySource {
            activitySource.requestAccess(config: config) { result in
                switch result {
                case .failure(let err):
                    completion(.failure(err))
                    return
                case .success:
                    self.apiClient.connect(trackerValue: activitySource.trackerValue) { connectionResult in
                        completion(connectionResult)
                    }
                }
            }
        } else {
            apiClient.connect(trackerValue: activitySource.trackerValue) { connectionResult in
                completion(connectionResult)
            }
        }
    }

    /// Disconnects the activity source connection and refreshes current connection list.
    /// In the case of HealthKitActivitySource it will disable backgroundDelivery
    /// - Parameters:
    ///   - activitySourceConnection: instance of ActivitySourceConnection
    ///   - completion: with status or error
    public func disconnect(activitySourceConnection: ActivitySourceConnection, completion: @escaping (Result<Bool, Error>) -> Void) {
        apiClient.disconnect(activitySourceConnection: activitySourceConnection) { result in
            switch result {
            case .success:
                activitySourceConnection.unmount { unmountResult in
                    switch unmountResult {
                    case .success:
                        self.mountedActivitySourceConnections = self.mountedActivitySourceConnections.filter { connection in
                            connection.tracker != activitySourceConnection.tracker
                        }
                        completion(.success(true))
                    case .failure(let err):
                        completion(.failure(err))
                    }
                }
            case .failure(let err):
                completion(.failure(err))
            }
        }
    }

    /// Returns a list of current connections of the user (from back-end) and mount/unmount activitySource if they do not exist in the local state.
    /// - Parameter completion: completion with [ActivitySourceConnection] or Error
    public func refreshCurrent(completion: @escaping (Result<[ActivitySourceConnection], Error>) -> Void) {
        apiClient.getCurrentConnections { result in
            switch result {
            case .success(let connections):
                let activitySourceConnections = connections.compactMap { connection in
                    return ActivitySourceConnectionFactory.activitySourceConnection(trackerConnection: connection)
                }
                self.refreshCurrentConnections(connections: connections)
                completion(.success(activitySourceConnections))
            case .failure(let err):
                completion(.failure(err))
            }
        }
    }

    /// Unmount all ActivitySources. Useful for logout from app case. The trackers will not be disconnected,
    /// but all locally mounted ActivitySources will be unmounted on the device. Currently only HealthKitActivitySource is mountable
    /// - Parameter completion: status or error
    public func unmout(completion: @escaping (Result<Bool, Error>) -> Void) {
        let group = DispatchGroup()
        var error: Error?

        self.mountedActivitySourceConnections.forEach { activitySourceConnection in

            group.enter()
            activitySourceConnection.unmount { result in
                switch result {
                case .success: break
                case .failure(let err):
                    error = err
                }

                group.leave()
            }
        }

        group.notify(queue: DispatchQueue.global()) {
            if let err = error {
                completion(.failure(err))
            } else {
                self.mountedActivitySourceConnections = []
                completion(.success(true))
            }
        }
    }

    private func refreshCurrentConnections(connections: [TrackerConnection]) {
        // Mount new trackers
        self.mountByConnections(connections: connections)

        // Unmount not relevant Trackers
        self.unmountByConnections(connections: connections)

        self.connectionsLocalStore.connections = connections
    }

    private func mountByConnections(connections: [TrackerConnection]) {
        connections.forEach { connection in
            if self.mountedActivitySourceConnections.contains(where: { element in element.tracker.value == connection.tracker }) {
              return
            }

            let activitySourceConnection = ActivitySourceConnectionFactory.activitySourceConnection(trackerConnection: connection)
            activitySourceConnection.mount(apiClient: apiClient, config: config, persistor: persistor) { result in
                switch result {
                case .success:
                    self.mountedActivitySourceConnections.append(activitySourceConnection)
                case .failure(let err):
                    logger.error("Error on mountByConnections \(err)")
                }
            }
        }
    }

    private func unmountByConnections(connections: [TrackerConnection]) {
        self.mountedActivitySourceConnections.forEach { activitySourceConnection in
            if !connections.contains(where: { element in element.tracker == activitySourceConnection.tracker.value }) {
                activitySourceConnection.unmount { result in
                    switch result {
                    case .success:
                        self.mountedActivitySourceConnections.removeAll { value in value.id == activitySourceConnection.id }
                    case .failure(let err):
                        logger.error("Error on unmout \(err)")
                    }
                }
            }
        }
    }

    private func restoreState(completion: (Result<Bool, Error>) -> Void) {
        connectionsLocalStore.connections?.forEach { connection in
            let activitySourceConnection = ActivitySourceConnectionFactory.activitySourceConnection(trackerConnection: connection)
            activitySourceConnection.mount(apiClient: apiClient, config: config, persistor: persistor) { result in
                switch result {
                case .success:
                    self.mountedActivitySourceConnections.append(activitySourceConnection)
                case .failure(let err):
                    logger.error("Error: on restore connectionsLocalStore state \(err)")
                }
            }
        }

        completion(.success(true))
    }
}
