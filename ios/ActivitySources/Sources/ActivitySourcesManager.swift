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

    public private(set) var mountedActivitySourceConnections: [ActivitySourceConnection] = []

    private let persistor: Persistor
    private let connectionsLocalStore: ActivitySourcesStateStore
    private let connectionFactory: (TrackerConnection) -> ActivitySourceConnection

    /// Restoring, applying refresh results, disconnecting and unmounting change the local state asynchronously,
    /// so they run one at a time.
    private let localStateChanges = SerialOperations()
    /// Advanced by `unmount()` and successful disconnects, so refreshes requested before them can't undo them.
    private let localStateGeneration = Counter()

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

        localStateChanges.enqueue { done in
            self.restoreState { result in
                done()
                completion?(result)
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
                self.localStateGeneration.increment()
                self.localStateChanges.enqueue { done in
                    activitySourceConnection.unmount { unmountResult in
                        switch unmountResult {
                        case .success:
                            self.mountedActivitySourceConnections = self.mountedActivitySourceConnections.filter { connection in
                                connection.tracker != activitySourceConnection.tracker
                            }
                            done()
                            completion(.success(()))
                        case .failure(let err):
                            done()
                            completion(.failure(err))
                        }
                    }
                }
            case .failure(let err):
                completion(.failure(err))
            }
        }
    }

    /// Returns a list of current connections of the user (from back-end) and mount/unmount activitySource if they do not exist in the local state.
    /// If `unmount` is called or a disconnect succeeds while the request is in flight, the fetched connections are returned but not applied locally.
    /// - Parameter completion: completion with [ActivitySourceConnection] or Error
    public func refreshCurrent(completion: @escaping (Result<[ActivitySourceConnection], Error>) -> Void) {
        let generationAtStart = localStateGeneration.value
        apiClient.getCurrentConnections { result in
            switch result {
            case .success(let connections):
                let activitySourceConnections = connections.map(self.connectionFactory)
                self.localStateChanges.enqueue { done in
                    // The response may predate an unmount (e.g. on logout) or a disconnect, so it must not change local state.
                    guard self.localStateGeneration.value == generationAtStart else {
                        done()
                        return completion(.success(activitySourceConnections))
                    }
                    self.refreshCurrentConnections(connections: connections) { result in
                        done()
                        completion(result.map { activitySourceConnections })
                    }
                }
            case .failure(let err):
                completion(.failure(err))
            }
        }
    }

    /// Unmount all ActivitySources. Useful for logout from app case. The trackers will not be disconnected,
    /// but all locally mounted ActivitySources will be unmounted on the device. Currently only HealthKitActivitySource is mountable.
    /// Waits for local state changes already in progress (e.g. a source being mounted) to finish first.
    /// - Parameter completion: void or error
    public func unmount(completion: @escaping (Result<Void, Error>) -> Void) {
        localStateGeneration.increment()
        localStateChanges.enqueue { done in
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
                    done()
                    completion(.failure(err))
                } else {
                    self.mountedActivitySourceConnections = []
                    done()
                    completion(.success(()))
                }
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

            let activitySourceConnection = self.connectionFactory(connection)
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

    private func restoreState(completion: @escaping (Result<Void, Error>) -> Void) {
        let group = DispatchGroup()
        var error: Error?
        connectionsLocalStore.connections?.forEach { connection in
            group.enter()
            let activitySourceConnection = self.connectionFactory(connection)
            activitySourceConnection.mount(apiClient: apiClient, config: config, persistor: persistor) { result in
                switch result {
                case .success:
                    self.mountedActivitySourceConnections.append(activitySourceConnection)
                case .failure(let err):
                    error = err
                    DataLogger.shared.error("Error: on restore connectionsLocalStore state \(err)")
                }
                group.leave()
            }
        }

        group.notify(queue: .global()) {
            if let err = error {
                completion(.failure(err))
            } else {
                completion(.success(()))
            }
        }
    }
}

/// Runs asynchronous operations one at a time, in the order they were enqueued.
/// Each operation must call `done` exactly once when it has finished.
private final class SerialOperations {
    typealias Operation = (_ done: @escaping () -> Void) -> Void

    private let lock = NSLock()
    private var pending: [Operation] = []
    private var isRunning = false

    func enqueue(_ operation: @escaping Operation) {
        lock.lock()
        pending.append(operation)
        let start = !isRunning
        isRunning = true
        lock.unlock()
        if start {
            runNext()
        }
    }

    private func runNext() {
        lock.lock()
        guard !pending.isEmpty else {
            isRunning = false
            lock.unlock()
            return
        }
        let operation = pending.removeFirst()
        lock.unlock()
        operation { self.runNext() }
    }
}

private final class Counter {
    private let lock = NSLock()
    private var count = 0

    var value: Int {
        lock.lock()
        defer { lock.unlock() }
        return count
    }

    func increment() {
        lock.lock()
        count += 1
        lock.unlock()
    }
}
