import Foundation
import FjuulCore
import FjuulActivitySources

/// Everything that belongs to one signed-in user. The api client never changes: switching users means signing out,
/// which discards the session together with every screen and model built on it.
final class Session: Identifiable {

    let id = UUID()
    let credentials: Credentials
    let apiClient: ApiClient
    let activitySourcesManager: ActivitySourcesManager
    let activitySources: ActivitySourceObservable

    private init(credentials: Credentials, apiClient: ApiClient, activitySourcesManager: ActivitySourcesManager) {
        self.credentials = credentials
        self.apiClient = apiClient
        self.activitySourcesManager = activitySourcesManager
        self.activitySources = ActivitySourceObservable(manager: activitySourcesManager)
    }

    /// Creates the api client and its activity sources manager. Completes on the main queue once the manager has restored
    /// the persisted connections (which sets up HealthKit background delivery), so nothing else uses it while it restores.
    static func start(credentials: Credentials, completion: @escaping (Session, Error?) -> Void) {
        let apiClient = ApiClient(
            baseUrl: credentials.baseUrl,
            apiKey: credentials.apiKey,
            credentials: UserCredentials(token: credentials.token, secret: credentials.secret)
        )
        // SDK consumer can not provide healthKitConfig if it's not used in-app
        let config = ActivitySourceConfigBuilder { builder in
            builder.healthKitConfig = HealthKitActivitySourceConfig(dataTypesToRead: [
                .activeEnergyBurned, .heartRate, .restingHeartRate,
                .distanceCycling, .distanceWalkingRunning,
                .stepCount, .workout, .height, .weight,
            ])
        }
        apiClient.initActivitySourcesManager(config: config) { result in
            DispatchQueue.main.async {
                // initActivitySourcesManager assigns the manager before it starts restoring.
                let session = Session(
                    credentials: credentials,
                    apiClient: apiClient,
                    activitySourcesManager: apiClient.activitySourcesManager!
                )
                // Restoring fails when a previously connected source can't be mounted again (e.g. HealthKit access was revoked).
                // The manager still works for every other source, and keeping the session lets sign-out unmount whatever did
                // get mounted, so the error is reported instead of blocking sign-in.
                if case .failure(let error) = result {
                    return completion(session, error)
                }
                completion(session, nil)
            }
        }
    }

    /// Disables HealthKit background delivery and deletes the SDK data persisted for this user.
    /// Completes on the main queue.
    func end(completion: @escaping (Error?) -> Void) {
        activitySourcesManager.unmount { result in
            DispatchQueue.main.async {
                if case .failure(let error) = result {
                    return completion(error)
                }
                completion(self.apiClient.clearPersistentStorage() ? nil : PersistentStorageNotClearedError())
            }
        }
    }
}

/// The credentials entered during onboarding, as persisted by `UserDefaultsManager`.
struct Credentials {
    let baseUrl: String
    let apiKey: String
    let token: String
    let secret: String

    static func stored() -> Credentials? {
        guard let token = UserDefaults.standard.string(forKey: "token"), !token.isEmpty,
              let secret = UserDefaults.standard.string(forKey: "secret"), !secret.isEmpty,
              let apiKey = UserDefaults.standard.string(forKey: "apiKey"), !apiKey.isEmpty,
              let environment = ApiEnvironment(rawValue: UserDefaults.standard.integer(forKey: "environment")) else {
            return nil
        }
        return Credentials(baseUrl: environment.baseUrl, apiKey: apiKey, token: token, secret: secret)
    }
}

private struct PersistentStorageNotClearedError: LocalizedError {
    var errorDescription: String? { "Could not clear the persistent storage" }
}
