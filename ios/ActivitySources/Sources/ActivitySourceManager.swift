import Foundation
import FjuulCore

final public class ActivitySourceManager {
    static public var current: ActivitySourceManager?

    var apiClient: ActivitySourcesApiClient
    var mountedActivitySourceConnections: [ActivitySourceConnection] = []
    var config: ActivitySourceConfigBuilder

    private var persistor: Persistor
    private var connectionsLocalStore: ActivitySourceStore

    static public func initialize(apiClient: ApiClient, config: ActivitySourceConfigBuilder) -> ActivitySourceManager {
        let instance = ActivitySourceManager(userToken: apiClient.userToken, persistor: apiClient.persistor, apiClient: apiClient.activitySources, config: config)

        ActivitySourceManager.current = instance

        return instance
    }

//    static public func getInstance() -> Result<ActivitySourceManager, Error> {
//        guard let instance = current else {
//            return .failure(FjuulError.invalidConfig)
//        }
//
//        return .success(instance)
//    }

    internal init(userToken: String, persistor: Persistor, apiClient: ActivitySourcesApiClient, config: ActivitySourceConfigBuilder) {
        self.apiClient = apiClient
        self.persistor = persistor
        self.config = config
        self.connectionsLocalStore = ActivitySourceStore(userToken: userToken, persistor: persistor)

        self.restoreState { _ in
            self.getCurrentConnections { _ in
                print("Initial sync current connections")
            }
        }
    }

    public func connect(activitySource: ActivitySource, completion: @escaping (Result<ConnectionResult, Error>) -> Void) {
        if let activitySource = activitySource as? MountableActivitySourceHK {
            activitySource.requestAccess(config: config) { result in
                switch result {
                case .failure(let err):
                    completion(.failure(err))
                    return
                case .success:
                    self.apiClient.connect(activitySourceItem: activitySource.tracker) { connectionResult in
                        completion(connectionResult)
                    }
                }
            }
        } else {
            apiClient.connect(activitySourceItem: activitySource.tracker) { connectionResult in
                completion(connectionResult)
            }
        }
    }

    public func disconnect(activitySourceConnection: ActivitySourceConnection, completion: @escaping (Result<Bool, Error>) -> Void) {
        apiClient.disconnect(activitySourceConnection: activitySourceConnection) { result in
            switch result {
            case .success:
                activitySourceConnection.unmount { unmountResult in
                    switch unmountResult {
                    case .success:
                        self.mountedActivitySourceConnections = self.mountedActivitySourceConnections.filter { connection in connection.tracker != activitySourceConnection.tracker }
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

    public func getCurrentConnections(completion: @escaping (Result<[ActivitySourceConnection], Error>) -> Void) {
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

    private func refreshCurrentConnections(connections: [TrackerConnection]) {
        // Mount new trackers
        self.mountByConnections(connections: connections)

        // Unmount not relevant Trackers
        self.unmountByConnections(connections: connections)

        self.connectionsLocalStore.connections = connections
    }

    private func mountByConnections(connections: [TrackerConnection]) {
        connections.forEach { connection in
            if self.mountedActivitySourceConnections.contains(where: { element in element.tracker?.rawValue == connection.tracker }) {
              return
            }

            if let activitySourceConnection = ActivitySourceConnectionFactory.activitySourceConnection(trackerConnection: connection) {
                activitySourceConnection.mount(apiClient: apiClient, config: config, persistor: persistor) { result in
                    switch result {
                    case .success:
                        self.mountedActivitySourceConnections.append(activitySourceConnection)
                    case .failure(let err):
                        print("Error on mountByConnections \(err)")
                    }
                }
            }
        }
    }

    private func unmountByConnections(connections: [TrackerConnection]) {
        self.mountedActivitySourceConnections.forEach { activitySourceConnection in
            if !connections.contains(where: { element in element.tracker == activitySourceConnection.tracker?.rawValue }) {
                activitySourceConnection.unmount { result in
                    switch result {
                    case .success:
                        self.mountedActivitySourceConnections.removeAll { value in value.id == activitySourceConnection.id }
                    case .failure(let err):
                        print("Error \(err)")
                    }
                }
            }
        }
    }

    private func restoreState(completion: (Result<Bool, Error>) -> Void) {
        connectionsLocalStore.connections?.forEach { connection in
            if let activitySourceConnection = ActivitySourceConnectionFactory.activitySourceConnection(trackerConnection: connection) {
                activitySourceConnection.mount(apiClient: apiClient, config: config, persistor: persistor) { result in
                    switch result {
                    case .success:
                        self.mountedActivitySourceConnections.append(activitySourceConnection)
                    case .failure(let err):
                        print("Error on restore connectionsLocalStore state \(err)")
                    }
                }
            }
        }

        completion(.success(true))
    }
}