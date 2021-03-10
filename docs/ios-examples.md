# iOS Examples

## User module
### Create user

``` swift
import FjuulCore
import FjuulUser

let baseUrl = "https://[env].api.fjuul.com"

let profileData = PartialUserProfile { profile in
    profile[\.birthDate] = DateFormatters.yyyyMMddLocale.date(from: "1989-11-03")
    profile[\.gender] = Gender.other
    profile[\.height] = 170
    profile[\.weight] = 60.6
}

ApiClient.createUser(baseUrl: baseUrl, apiKey: "YOUR_API_KEY", profile: profileData) { result in
    switch result {
    case .success(let creationResult):
      // You will need to persist token/secret
      // creationResult.user.token
      // creationResult.secret
    case .failure(let err):
      // handle error
    }
}
```

The result of the creation is an instance of `UserCreationResult`, which is a composition of the user profile and secret of the user. You should persist the token and secret of the user.  
To perform user-authorized actions, you must provide the user credentials to the `ApiClient` initializer. The user credentials are a pair of token and secret.

## Core Module
### Initialize ApiClient
In order to use the Fjuul SDK API you need to initialize an `ApiClient` instance from the **Core** module:

``` swift
import FjuulCore

let baseUrl = "https://[env].api.fjuul.com"

// initialize Fjuul ApiClient with user credentials
let apiClient = ApiClient(
    baseUrl: baseUrl,
    apiKey: "YOUR_API_KEY",
    credentials: UserCredentials(
        token: "USER_TOKEN",
        secret: "USER_SECRET"
    )
)
```

## Activity Sources Module
### Initialize ActivitySourcesManager

ActivitySourcesManager should be initialized once as early as possible in your app lifecycle, for example in AppDelegate. This is required for fetching intraday data from HealthKit through background delivery.

``` swift
import FjuulActivitySources

class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {

        // Init apiClient with user credentials before initializing ActivitySourcesManager (see example in `Initialize ApiClient`)
        ...

        let config = ActivitySourceConfigBuilder { builder in
            builder.healthKitConfig = HealthKitActivitySourceConfig(dataTypesToRead: [
                .activeEnergyBurned, .heartRate,
                .distanceCycling, .distanceWalkingRunning,
                .stepCount, .workout, .height, .bodyMass
            ])
        }

        apiClient.initActivitySourcesManager(config: config)
    }
}
```

After calling `apiClient.initActivitySourcesManager`, the `ActivitySourceManager` instance will be available as `apiClient.activitySourcesManager`.

### Collect HealthKit Data

HealthKitActivitySource will register background delivery observers after connecting. HealthKit will then trigger the app each time when new data (corresponding to the types from HealthKitActivitySourceConfig) was stored in HealthKit.

### Connect ActivitySource

``` swift
import FjuulActivitySources

apiClient.activitySourcesManager?.connect(activitySource: HealthKitActivitySource.shared) { result in
    switch result {
    case .success(let connectionResult):
        switch connectionResult {
        case .connected:

          // After connect or disconnect always call ActivitySourcesManager.refreshCurrent() for mount new and unmount locally new and removed ActivitySource's.
          apiClient.activitySourcesManager?.refreshCurrent { result in
              switch result {
              case .success(let connections):
                  // Array of current ActivitySourceConnection's
              case .failure(let err):
                  // handle error
              }
          }

        case .externalAuthenticationFlowRequired(let authenticationUrl):
            guard let url = URL(string: authenticationUrl) else { return }
            UIApplication.shared.open(url)
        }
    case .failure(let err):
        // handle error
    }
}
```

For external trackers like Polar, Garmin, Suunto, the ConnectionResult will expose an `authenticationUrl` that needs to be opened in an external browser, where the user will have to complete the vendors OAuth flow. At the end of the auth flow, the user will be redirected to the app through a deep link. To handle this deep link the SDK provides the utility class `ExternalAuthenticationFlowHandler`:

``` swift
import FjuulActivitySources

let connectionStatus = ExternalAuthenticationFlowHandler.handle(url: url)
if connectionStatus.status {
    // Update activitySource list
    apiClient.activitySourcesManager?.refreshCurrent { result in
        switch result {
        case .success(let connections):
            // Array of current ActivitySourceConnection's
        case .failure(let err):
            // handle error
        }
    }
}
```

## Analytics Module
### Get DailyStats

``` swift
import FjuulAnalytics

// Set period
let fromDate = Calendar.current.date(byAdding: .day, value: -7, to: Date())
let toDate = Date()

apiClient.analytics.dailyStats(from: fromDate, to: toDate) { result in
    switch result {
    case .success(let dailyStats):
        // dailyStats is array of DailyStats objects per requested days
    case .failure(let err):
        // handle error
    }
}
```
