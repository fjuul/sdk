import Foundation
import FjuulCore

/**
 The ActivitySourcesManager encapsulates a connection to fitness trackers, access to current user's tracker connections.
 This is a high-level entity and entry point of the ActivitySources module.

 One of the main functions of this module is to connect to activity sources. There are local (i.e. HealthKit) and external trackers (i.e. Polar, Garmin, Google Health, etc).
 External trackers require user authentication in the web browser.

 For handle authentication with external trackers, it require support deep linking in app.
*/
final public class ActivitySourcesManager {
    let apiClient: ActivitySourcesApiClient
    let config: ActivitySourceConfigBuilder

    /// Connections whose activity sources are currently mounted on this device.
    public var mountedActivitySourceConnections: [ActivitySourceConnection] {
        return stateQueue.sync { mountedConnections }
    }

    private let persistor: Persistor
    private let connectionsLocalStore: ActivitySourcesStateStore
    private let connectionFactory: (TrackerConnection) -> ActivitySourceConnection

    /// Owns all local state below. Callbacks of mounting and unmounting hop onto it before touching that state.
    private let stateQueue = DispatchQueue(label: "com.fjuul.sdk.activitysources.ActivitySourcesManager")
    private var mountedConnections: [ActivitySourceConnection] = []
    /// Advanced by `unmount()` and successful disconnects, so refreshes requested before them can't undo them.
    private var generation = 0
    /// Local state changes complete asynchronously, so they run one at a time in the order they were requested.
    private var pendingStateChanges: [() -> Void] = []
    private var isChangingState = false

    /// Internal initializer.
    ///
    /// - Parameters:
    ///   - userToken: User token
    ///   - persistor: for persisted data like HealthKit anchors
    ///   - apiClient: configured client of ActivitySourcesApiClient
    ///   - config: Activity source config
    ///   - connectionFactory: Creates the activity source connection for a tracker connection
    ///   - completion: Optional completion block will be called when the local state is restored
    internal init(userToken: String,
                  persistor: Persistor,
                  apiClient: ActivitySourcesApiClient,
                  config: ActivitySourceConfigBuilder,
                  connectionFactory: @escaping (TrackerConnection) -> ActivitySourceConnection =
                    ActivitySourceConnectionFactory.activitySourceConnection(trackerConnection:),
                  completion: ((Result<Void, Error>) -> Void)? = nil) {

        self.apiClient = apiClient
        self.persistor = persistor
        self.config = config
        self.connectionsLocalStore = ActivitySourcesStateStore(userToken: userToken, persistor: persistor)
        self.connectionFactory = connectionFactory

        changeState({ finish in
            self.reconcile(to: self.connectionsLocalStore.connections ?? [], persisting: false, completion: finish)
        }, completion: { error in
            completion?(error.map { .failure($0) } ?? .success(()))
        })
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

    /// Disconnects the activity source connection and removes it from the local state.
    /// In the case of HealthKitActivitySource it will disable backgroundDelivery.
    /// - Parameters:
    ///   - activitySourceConnection: instance of ActivitySourceConnection
    ///   - completion: with void or error
    public func disconnect(activitySourceConnection: ActivitySourceConnection, completion: @escaping (Result<Void, Error>) -> Void) {
        apiClient.disconnect(activitySourceConnection: activitySourceConnection) { result in
            if case .failure(let err) = result {
                return DispatchQueue.global().async { completion(.failure(err)) }
            }
            let tracker = activitySourceConnection.tracker.value
            self.changeState(invalidatingEarlierRefreshes: true, { finish in
                self.connectionsLocalStore.connections = self.connectionsLocalStore.connections?.filter { $0.tracker != tracker }
                self.unmountSequentially(self.mountedConnections.filter { $0.tracker.value == tracker }, completion: finish)
            }, completion: { error in
                completion(error.map { .failure($0) } ?? .success(()))
            })
        }
    }

    /// Returns a list of current connections of the user (from back-end) and mount/unmount activitySource if they do not exist in the local state.
    /// If `unmount` is called or a disconnect succeeds while the request is in flight, the fetched connections are returned but not applied locally.
    /// - Parameter completion: completion with [ActivitySourceConnection] or Error
    public func refreshCurrent(completion: @escaping (Result<[ActivitySourceConnection], Error>) -> Void) {
        stateQueue.async {
            let generationAtStart = self.generation
            self.apiClient.getCurrentConnections { result in
                switch result {
                case .success(let connections):
                    let activitySourceConnections = connections.map(self.connectionFactory)
                    self.changeState({ finish in
                        // The response may predate an unmount (e.g. on logout) or a disconnect, so it must not change local state.
                        guard self.generation == generationAtStart else { return finish(nil) }
                        self.reconcile(to: connections, completion: finish)
                    }, completion: { error in
                        completion(error.map { .failure($0) } ?? .success(activitySourceConnections))
                    })
                case .failure(let err):
                    DispatchQueue.global().async { completion(.failure(err)) }
                }
            }
        }
    }

    /// Unmount all ActivitySources. Useful for logout from app case. The trackers will not be disconnected,
    /// but all locally mounted ActivitySources will be unmounted on the device. Currently only HealthKitActivitySource is mountable.
    /// Waits for local state changes already in progress (e.g. a source being mounted) to finish first.
    /// - Parameter completion: void or error
    public func unmount(completion: @escaping (Result<Void, Error>) -> Void) {
        changeState(invalidatingEarlierRefreshes: true, { finish in
            self.unmountSequentially(self.mountedConnections, completion: finish)
        }, completion: { error in
            completion(error.map { .failure($0) } ?? .success(()))
        })
    }

    // MARK: - Local state (only accessed on `stateQueue`)

    /// Runs `change` on the state queue once all earlier changes have finished, then calls `completion` off that queue.
    /// `change` must call `finish` exactly once, on the state queue.
    private func changeState(invalidatingEarlierRefreshes: Bool = false,
                             _ change: @escaping (_ finish: @escaping (Error?) -> Void) -> Void,
                             completion: @escaping (Error?) -> Void) {
        stateQueue.async {
            if invalidatingEarlierRefreshes {
                self.generation += 1
            }
            self.pendingStateChanges.append {
                change { error in
                    self.isChangingState = false
                    self.startNextStateChange()
                    DispatchQueue.global().async { completion(error) }
                }
            }
            self.startNextStateChange()
        }
    }

    private func startNextStateChange() {
        guard !isChangingState, !pendingStateChanges.isEmpty else { return }
        isChangingState = true
        pendingStateChanges.removeFirst()()
    }

    /// Persists `desired` (unless restoring from it), unmounts mounted sources that aren't in it, then mounts the missing ones.
    /// Completes on the state queue with the first error; later steps still run.
    private func reconcile(to desired: [TrackerConnection], persisting: Bool = true, completion: @escaping (Error?) -> Void) {
        if persisting {
            connectionsLocalStore.connections = desired
        }
        let obsolete = mountedConnections.filter { mounted in !desired.contains { $0.tracker == mounted.tracker.value } }
        unmountSequentially(obsolete) { unmountError in
            let missing = desired.filter { connection in !self.mountedConnections.contains { $0.tracker.value == connection.tracker } }
            self.mountSequentially(missing.map(self.connectionFactory)) { mountError in
                completion(unmountError ?? mountError)
            }
        }
    }

    private func mountSequentially(_ connections: [ActivitySourceConnection], completion: @escaping (Error?) -> Void) {
        sequentially(connections, completion: completion) { connection, finish in
            connection.mount(apiClient: self.apiClient, config: self.config, persistor: self.persistor) { result in
                self.stateQueue.async {
                    switch result {
                    case .success:
                        self.mountedConnections.append(connection)
                        finish(nil)
                    case .failure(let err):
                        DataLogger.shared.error("Error on mounting \(connection.tracker.value): \(err)")
                        finish(err)
                    }
                }
            }
        }
    }

    private func unmountSequentially(_ connections: [ActivitySourceConnection], completion: @escaping (Error?) -> Void) {
        sequentially(connections, completion: completion) { connection, finish in
            connection.unmount { result in
                self.stateQueue.async {
                    switch result {
                    case .success:
                        self.mountedConnections.removeAll { $0.id == connection.id }
                        finish(nil)
                    case .failure(let err):
                        DataLogger.shared.error("Error on unmounting \(connection.tracker.value): \(err)")
                        finish(err)
                    }
                }
            }
        }
    }

    /// Runs `step` for each element, one after another; completes with the first error after all steps ran.
    private func sequentially<Element>(_ elements: [Element], firstError: Error? = nil, completion: @escaping (Error?) -> Void,
                                       step: @escaping (Element, @escaping (Error?) -> Void) -> Void) {
        guard let element = elements.first else {
            return completion(firstError)
        }
        step(element) { error in
            self.sequentially(Array(elements.dropFirst()), firstError: firstError ?? error, completion: completion, step: step)
        }
    }
}
