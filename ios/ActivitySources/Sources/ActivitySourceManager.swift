import Foundation
import FjuulCore

final public class ActivitySourceManager {
    static public let shared = ActivitySourceManager()

    var apiClient: ApiClient?
    var mountedActivitySourceConnections: [ActivitySourceConnection] = []
    var config: ActivitySourceConfigBuilder?

    private var persistor: Persistor?
    private var connectionsLocalStore: ActivitySourceStore?

    private init() {}

    public func initialize(apiClient: ApiClient, config: ActivitySourceConfigBuilder, persistor: Persistor = DiskPersistor()) {
        self.apiClient = apiClient
        self.persistor = persistor
        self.config = config

        self.connectionsLocalStore = ActivitySourceStore(userToken: apiClient.userToken, persistor: persistor)

        self.restoreState { _ in
            self.getCurrentConnections { _ in
                print("Initial sync current connections")
            }
        }
    }

    public func connect(activitySource: ActivitySource, completion: @escaping (Result<ConnectionResult, Error>) -> Void) {
        guard let apiClient = self.apiClient else {
            completion(.failure(FjuulError.invalidConfig))
            return
        }

        guard let config = self.config else {
            completion(.failure(FjuulError.invalidConfig))
            return
        }

        // TODO Refactoring
        if activitySource is ActivitySourceHK {
            ActivitySourceHK.requestAccess(config: config) { result in
                switch result {
                case .failure(let err):
                    completion(.failure(err))
                    return
                case .success:
                    apiClient.activitySources.connect(activitySourceItem: activitySource.tracker) { connectionResult in
                        completion(connectionResult)
                    }
                }
            }
        } else {
            apiClient.activitySources.connect(activitySourceItem: activitySource.tracker) { connectionResult in
                completion(connectionResult)
            }
        }
    }

    public func disconnect(activitySourceConnection: ActivitySourceConnection, completion: @escaping (Result<Void, Error>) -> Void) {
        guard let apiClient = self.apiClient else {
            completion(.failure(FjuulError.invalidConfig))
            return
        }

        apiClient.activitySources.disconnect(activitySourceConnection: activitySourceConnection) { result in
            completion(result)
        }
    }

    public func getCurrentConnections(completion: @escaping (Result<[ActivitySourceConnection], Error>) -> Void) {
        guard let apiClient = self.apiClient else {
            completion(.failure(FjuulError.invalidConfig))
            return
        }

        apiClient.activitySources.getCurrentConnections { result in
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

        self.connectionsLocalStore?.connections = connections
    }

    private func mountByConnections(connections: [TrackerConnection]) {
        guard let apiClient = self.apiClient else { return }
        guard let persistor = self.persistor else { return }
        guard let config = self.config else { return }

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
        guard let apiClient = self.apiClient else {
            completion(.failure(FjuulError.invalidConfig))
            return
        }
        guard let persistor = self.persistor else {
            completion(.failure(FjuulError.invalidConfig))
            return
        }
        guard let config = self.config else {
            completion(.failure(FjuulError.invalidConfig))
            return
        }

        connectionsLocalStore?.connections?.forEach { connection in
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
