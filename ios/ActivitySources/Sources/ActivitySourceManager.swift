import Foundation
import FjuulCore

final public class ActivitySourceManager {
    static public let shared = ActivitySourceManager()

    var apiClient: ApiClient?
    var mountedActivitySourceConnections: [ActivitySourceConnection] = []

    private var persistor: Persistor?
    private var connectionsLocalStore: ActivitySourceStore?

    private init() {}

    public func initialize(apiClient: ApiClient, persistor: Persistor = DiskPersistor()) {
        self.apiClient = apiClient
        self.persistor = persistor

        self.connectionsLocalStore = ActivitySourceStore(userToken: apiClient.userToken, persistor: persistor)

        self.restoreState()
        self.refreshCurrentConnections()
    }

    public func connect(activitySource: ActivitySource, completion: @escaping (Result<ConnectionResult, Error>) -> Void) {
        guard let apiClient = self.apiClient else {
            completion(.failure(FjuulError.invalidConfig))
            return
        }

        /// Request permissions from HealthKit
        if activitySource is ActivitySourceHK {
            ActivitySourceHK.requestAccess { result in
                switch result {
                case .failure(let err):
                    completion(.failure(err))
                    return
                case .success:
                    print("ActivitySourceHK.requestAccess success")
                }
            }
        }

        apiClient.activitySources.connect(activitySource: "healthkit") { connectionResult in
            completion(connectionResult)
        }
    }

    private func restoreState() {
        guard let apiClient = self.apiClient else { return }
        guard let persistor = self.persistor else { return }

        connectionsLocalStore?.connections?.forEach { connection in
            if let activitySourceConnection = ActivitySourceConnectionFactory.activitySourceConnection(trackerConnection: connection) {
                activitySourceConnection.mount(apiClient: apiClient, persistor: persistor) { result in
                    switch result {
                    case .success:
                        self.mountedActivitySourceConnections.append(activitySourceConnection)
                    case .failure(let err):
                        print("Error on restore connectionsLocalStore state \(err)")
                    }
                }
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
