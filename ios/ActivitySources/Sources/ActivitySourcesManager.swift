import Foundation
import FjuulCore

/**
 The ActivitySourcesManager encapsulates a connection to fitness trackers, access to current user's tracker connections.
 This is a high-level entity and entry point of the ActivitySources module.

 One of the main functions of this module is to connect to activity sources. There are local (i.e. HealthKit) and external trackers (i.e. Polar, Garmin, Fitbit, etc).
 External trackers require user authentication in the web browser.
 
 For handle authentication with external trackers, it require support deep linking in app.
*/
final public class ActivitySourcesManager {
    let apiClient: ActivitySourcesApiClient
    let config: ActivitySourceConfigBuilder

    public private(set) var mountedActivitySourceConnections: [ActivitySourceConnection] = []

    private let persistor: Persistor
    private let connectionsLocalStore: ActivitySourcesStateStore

    /// Internal initializer.
    ///
    /// - Parameters:
    ///   - userToken: User token
    ///   - persistor: for persisted data like HealthKit anchors
    ///   - apiClient: configured client of ActivitySourcesApiClient
    ///   - config: Activity source config
    ///   - completion: Optional completion block will be called when the local state is restored
    internal init(userToken: String,
                  persistor: Persistor,
                  apiClient: ActivitySourcesApiClient,
                  config: ActivitySourceConfigBuilder,
                  completion: ((Result<Void, Error>) -> Void)? = nil) {

        self.apiClient = apiClient
        self.persistor = persistor
        self.config = config
        self.connectionsLocalStore = ActivitySourcesStateStore(userToken: userToken, persistor: persistor)

        self.restoreState { result in
            if let completion = completion {
                completion(result)
            }
        }
    }

    /// Connect specified ActivitySource.
    /// 
    /// # ConnectionResult can have 2 states:
    /// 1) connected: it will contain TrackerConnection data
    /// 2) authenticationUrl: it will contain authenticationUrl, the SDK consumer needs to open that URL in the browser to allow the user to finish
    /// authentication (Garmin, Polar, etc...)
    ///
    /// Healthkit tracker will show an iOS modal prompting with a list of required Healthkit data permissions.
    /// After a user succeeds in the connection, please invoke refreshing current connections `ActivitySourcesManager#getCurrentConnections`
    /// - Parameters:
    ///   - activitySource: ActivitySource instance to connect (PolarActivitySource.shared, HealthKitActivitySource.shared, etc...)
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
    /// In the case of HealthKitActivitySource it will disable backgroundDelivery.
    /// - Parameters:
    ///   - activitySourceConnection: instance of ActivitySourceConnection
    ///   - completion: with void or error
    public func disconnect(activitySourceConnection: ActivitySourceConnection, completion: @escaping (Result<Void, Error>) -> Void) {
        apiClient.disconnect(activitySourceConnection: activitySourceConnection) { result in
            switch result {
            case .success:
                activitySourceConnection.unmount { unmountResult in
                    switch unmountResult {
                    case .success:
                        self.mountedActivitySourceConnections = self.mountedActivitySourceConnections.filter { connection in
                            connection.tracker != activitySourceConnection.tracker
                        }
                        completion(.success(()))
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
                self.refreshCurrentConnections(connections: connections) { result in
                    switch result {
                    case .success:
                        completion(.success(activitySourceConnections))
                    case .failure(let err):
                        completion(.failure(err))
                    }
                }
            case .failure(let err):
                completion(.failure(err))
            }
        }
    }

    /// Unmount all ActivitySources. Useful for logout from app case. The trackers will not be disconnected,
    /// but all locally mounted ActivitySources will be unmounted on the device. Currently only HealthKitActivitySource is mountable
    /// - Parameter completion: void or error
    public func unmount(completion: @escaping (Result<Void, Error>) -> Void) {
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
                completion(.success(()))
            }
        }
    }

    private func refreshCurrentConnections(connections: [TrackerConnection], completion: @escaping (Result<Void, Error>) -> Void) {
        let group = DispatchGroup()
        var error: Error?

        // Mount new trackers
        group.enter()
        self.mountByConnections(connections: connections) { result in
            switch result {
            case .success: break
            case .failure(let err):
                error = err
            }

            group.leave()
        }

        // Unmount not relevant Trackers
        group.enter()
        self.unmountByConnections(connections: connections) { result in
            switch result {
            case .success: break
            case .failure(let err):
                error = err
            }

            group.leave()
        }

        self.connectionsLocalStore.connections = connections

        group.notify(queue: DispatchQueue.global()) {
            if let err = error {
                completion(.failure(err))
            } else {
                completion(.success(()))
            }
        }
    }

    private func mountByConnections(connections: [TrackerConnection], completion: @escaping (Result<Void, Error>) -> Void) {
        let group = DispatchGroup()
        var error: Error?

        connections.forEach { connection in
            if self.mountedActivitySourceConnections.contains(where: { element in element.tracker.value == connection.tracker }) {
              return
            }

            group.enter()

            let activitySourceConnection = ActivitySourceConnectionFactory.activitySourceConnection(trackerConnection: connection)
            activitySourceConnection.mount(apiClient: apiClient, config: config, persistor: persistor) { result in
                switch result {
                case .success:
                    self.mountedActivitySourceConnections.append(activitySourceConnection)
                case .failure(let err):
                    error = err
                    DataLogger.shared.error("Error on mountByConnections \(err)")
                }
                group.leave()
            }
        }

        group.notify(queue: DispatchQueue.global()) {
            if let err = error {
                completion(.failure(err))
            } else {
                completion(.success(()))
            }
        }
    }

    private func unmountByConnections(connections: [TrackerConnection], completion: @escaping (Result<Void, Error>) -> Void) {
        let group = DispatchGroup()
        var error: Error?

        self.mountedActivitySourceConnections.forEach { activitySourceConnection in
            if !connections.contains(where: { element in element.tracker == activitySourceConnection.tracker.value }) {
                group.enter()

                activitySourceConnection.unmount { result in
                    switch result {
                    case .success:
                        self.mountedActivitySourceConnections.removeAll { value in value.id == activitySourceConnection.id }
                    case .failure(let err):
                        error = err
                        DataLogger.shared.error("Error on unmount \(err)")
                    }

                    group.leave()
                }
            }
        }

        group.notify(queue: DispatchQueue.global()) {
            if let err = error {
                completion(.failure(err))
            } else {
                completion(.success(()))
            }
        }
    }

    private func restoreState(completion: (Result<Void, Error>) -> Void) {
        connectionsLocalStore.connections?.forEach { connection in
            let activitySourceConnection = ActivitySourceConnectionFactory.activitySourceConnection(trackerConnection: connection)
            activitySourceConnection.mount(apiClient: apiClient, config: config, persistor: persistor) { result in
                switch result {
                case .success:
                    self.mountedActivitySourceConnections.append(activitySourceConnection)
                case .failure(let err):
                    DataLogger.shared.error("Error: on restore connectionsLocalStore state \(err)")
                }
            }
        }

        completion(.success(()))
    }
}
