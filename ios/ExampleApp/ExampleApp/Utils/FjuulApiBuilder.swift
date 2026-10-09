import Foundation
import FjuulCore
import FjuulActivitySources

/// Prepare API client and configure ActivitySourcesManager
/// Should be initialized once as soon as possible after up app or after create a user, for setup backgroundDelivery for the HealthKit to fetch intraday data,
/// for example in AppDelegate (didFinishLaunchingWithOptions)
class FjuulApiBuilder {

    struct ApiClientConfig: Equatable {
        let baseUrl: String
        let apiKey: String
        let token: String
        let secret: String
    }

    /// Operations queued while a set-up or tear-down is running; nil when none is running. Only accessed on the main queue.
    private static var queuedWhileTransitioning: [() -> Void]?

    /// An unpublished client whose activity sources could not be unmounted; cleanup is retried before the next set-up or tear-down.
    private static var unpublishedClientNeedingCleanup: ApiClient?

    /// Makes `ApiClientHolder` hold a client for the stored credentials, replacing a client built for other credentials.
    /// The client is only published once its activity sources manager has restored its persisted connections.
    /// Must be called on the main queue; completion is called on the main queue.
    static func setUpApiClient(completion: @escaping (Error?) -> Void) {
        serialized { done in
            let finish = { (error: Error?) in
                completion(error)
                done()
            }
            guard let config = storedConfig(), config != ApiClientHolder.default.config else {
                return finish(nil)
            }
            tearDownCurrentApiClient(clearPersistentStorage: false) { error in
                if let error = error {
                    return finish(error)
                }
                let apiClient = ApiClient(
                    baseUrl: config.baseUrl,
                    apiKey: config.apiKey,
                    credentials: UserCredentials(token: config.token, secret: config.secret)
                )
                buildActivitySourcesManager(apiClient: apiClient) { result in
                    DispatchQueue.main.async {
                        switch result {
                        case .success:
                            ApiClientHolder.default.set(apiClient, config: config)
                            finish(nil)
                        case .failure(let restoreError):
                            guard let manager = apiClient.activitySourcesManager else {
                                return finish(restoreError)
                            }
                            // Don't leave background delivery set up for a client that is never published.
                            manager.unmount { unmountResult in
                                DispatchQueue.main.async {
                                    if case .failure(let unmountError) = unmountResult {
                                        unpublishedClientNeedingCleanup = apiClient
                                        return finish(CleanupAfterRestoreFailedError(restoreError: restoreError, unmountError: unmountError))
                                    }
                                    finish(restoreError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /// Unmounts activity sources (disabling HealthKit background delivery) before releasing the current client.
    /// Must be called on the main queue; completion is called on the main queue.
    static func tearDownApiClient(clearPersistentStorage: Bool, completion: @escaping (Error?) -> Void) {
        serialized { done in
            tearDownCurrentApiClient(clearPersistentStorage: clearPersistentStorage) { error in
                completion(error)
                done()
            }
        }
    }

    /// Runs `operation` once no other set-up or tear-down is running; it must call `done` exactly once when finished.
    private static func serialized(_ operation: @escaping (_ done: @escaping () -> Void) -> Void) {
        if queuedWhileTransitioning != nil {
            queuedWhileTransitioning?.append { serialized(operation) }
            return
        }
        queuedWhileTransitioning = []
        operation {
            let queued = queuedWhileTransitioning ?? []
            queuedWhileTransitioning = nil
            queued.forEach { $0() }
        }
    }

    private static func tearDownCurrentApiClient(clearPersistentStorage: Bool, completion: @escaping (Error?) -> Void) {
        if let manager = unpublishedClientNeedingCleanup?.activitySourcesManager {
            manager.unmount { result in
                DispatchQueue.main.async {
                    if case .failure(let err) = result {
                        return completion(err)
                    }
                    unpublishedClientNeedingCleanup = nil
                    tearDownCurrentApiClient(clearPersistentStorage: clearPersistentStorage, completion: completion)
                }
            }
            return
        }
        guard let apiClient = ApiClientHolder.default.apiClient else {
            return completion(nil)
        }
        let release = {
            if clearPersistentStorage && !apiClient.clearPersistentStorage() {
                return completion(PersistentStorageNotClearedError())
            }
            ApiClientHolder.default.set(nil, config: nil)
            completion(nil)
        }
        guard let manager = apiClient.activitySourcesManager else {
            return release()
        }
        manager.unmount { result in
            DispatchQueue.main.async {
                if case .failure(let err) = result {
                    return completion(err)
                }
                release()
            }
        }
    }

    private static func storedConfig() -> ApiClientConfig? {
        guard let token = UserDefaults.standard.string(forKey: "token"), !token.isEmpty,
              let secret = UserDefaults.standard.string(forKey: "secret"), !secret.isEmpty,
              let apiKey = UserDefaults.standard.string(forKey: "apiKey"), !apiKey.isEmpty,
              let environment = ApiEnvironment(rawValue: UserDefaults.standard.integer(forKey: "environment")) else {
            return nil
        }
        return ApiClientConfig(baseUrl: environment.baseUrl, apiKey: apiKey, token: token, secret: secret)
    }

    private static func buildActivitySourcesManager(apiClient: ApiClient, completion: @escaping (Result<Void, Error>) -> Void) {
        // SDK consumer can not provide healthKitConfig if it's not used in-app
        let config = ActivitySourceConfigBuilder { builder in
            builder.healthKitConfig = HealthKitActivitySourceConfig(dataTypesToRead: [
                .activeEnergyBurned, .heartRate, .restingHeartRate,
                .distanceCycling, .distanceWalkingRunning,
                .stepCount, .workout, .height, .weight
            ])
        }

        apiClient.initActivitySourcesManager(config: config, completion: completion)
    }
}

private struct PersistentStorageNotClearedError: LocalizedError {
    var errorDescription: String? { "Could not clear the persistent storage" }
}

private struct CleanupAfterRestoreFailedError: LocalizedError {
    let restoreError: Error
    let unmountError: Error

    var errorDescription: String? {
        "Restoring activity sources failed (\(restoreError.localizedDescription)), and unmounting them afterwards failed too "
            + "(\(unmountError.localizedDescription)). HealthKit background delivery may still be enabled; continuing again retries the cleanup."
    }
}
