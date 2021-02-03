import Foundation
import FjuulCore
import FjuulActivitySources

/// Prepare API cleint and configure ActivitySourceManager
/// Should be Initialize once as soon as possible after up app or after create a user, for setup backgroundDelivery for the HealthKit to fetch intraday data,
/// for example in AppDelegate (didFinishLaunchingWithOptions)
class FjuulApiBuilder {

    static func buildApiClient() -> ApiClient? {
        if self.enoughApiClientParams() {
            let environment = ApiEnvironment(rawValue: UserDefaults.standard.integer(forKey: "environment"))!

            return ApiClient(
                baseUrl: environment.baseUrl,
                apiKey: UserDefaults.standard.string(forKey: "apiKey") ?? "",
                credentials: UserCredentials(
                    token: UserDefaults.standard.string(forKey: "token") ?? "",
                    secret: UserDefaults.standard.string(forKey: "secret") ?? ""
                )
            )
        }

        return nil
    }

    static func buildActivitySourceManager(apiClient: ApiClient) {
        // SDK consumer can not provide healthKitConfig if it's not used in-app
        let config = ActivitySourceConfigBuilder { builder in
            builder.healthKitConfig = HealthKitActivitySourceConfig(dataTypesToRead: [
                                                                .activeEnergyBurned, .heartRate,
                                                                .distanceCycling, .distanceWalkingRunning,
                                                                .stepCount, .workout,
            ])
        }

        apiClient.initActivitySourcesManager(config: config)
    }

    private static func enoughApiClientParams() -> Bool {
        guard UserDefaults.standard.string(forKey: "token") != nil else {
            return false
        }

        guard UserDefaults.standard.string(forKey: "secret") != nil else {
            return false
        }

        guard UserDefaults.standard.string(forKey: "apiKey") != nil else {
            return false
        }

        return true
    }
}
