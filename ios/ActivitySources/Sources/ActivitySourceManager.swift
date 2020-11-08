import Foundation
import FjuulCore

final public class ActivitySourceManager {
    static public let shared = ActivitySourceManager()

    var persistor: Persistor?
    var apiClient: ApiClient?
    var connectionsLocalStore: ActivitySourceStore?

    private init() {}

    public func initialize(apiClient: ApiClient, persistor: Persistor = DiskPersistor()) {
        self.apiClient = apiClient
        self.persistor = persistor

        self.connectionsLocalStore = ActivitySourceStore(userToken: apiClient.userToken, persistor: persistor)

        self.restoreState()
        self.refreshCurrentConnections()
    }

    private func restoreState() {
        guard let apiClient = self.apiClient else { return }
        guard let persistor = self.persistor else { return }

        connectionsLocalStore?.connections?.forEach { connection in
            if let activitySourceConnection = ActivitySourceConnectionFactory.activitySourceConnection(trackerConnection: connection) {
                _ = activitySourceConnection.mount(apiClient: apiClient, persistor: persistor)
            }
        }
    }

    private func refreshCurrentConnections() {
        self.apiClient?.activitySources.getCurrentConnections { result in
            switch result {
            case .success(let connections):
                // TODO: Mount/Unmout new or not used connections
                self.connectionsLocalStore?.connections = connections
            case .failure(let err):
                print("Error updateCurrentConnections ", err)
            }
        }
    }
}
