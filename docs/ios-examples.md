# iOS Examples

## User module

### Create user

``` swift
import FjuulCore
import FjuulUser

let baseUrl = "https://xxx.api.fjuul.com"

let profileData = PartialUserProfile([
    UserProfile.birthDate: birthDate,
    UserProfile.gender: gender,
    UserProfile.height: height,
    UserProfile.weight: weight,
    UserProfile.timezone: timezone,
    UserProfile.locale: locale,
])

ApiClient.createUser(baseUrl: baseUrl, apiKey: "YOUR_API_KEY", profile: profileData) { result in
    switch result {
    case .success(let creationResult):
      // You will need save token/secret in the persisted store
      // creationResult.user.token
      // creationResult.secret
    case .failure(let err):
      // handle error
    }
}
```

The result of the creation is an instance of `UserCreationResult` class which is a composition of the user profile and secret of the user. You should save the token and secret of the user.  
To perform user-authorized actions, you must provide the user credentials to the `ApiClient` initializer. The user credentials are a pair of token and secret.

## Core module
### Initialize of API Client
In order to use Fjuul SDK API you need to initialize `ApiClient` from **Core** module:

``` swift
import FjuulCore

let baseUrl = "https://xxx.api.fjuul.com"

// initialize Fjuul ApiClient with user credentials
let apiCleint = ApiClient(
    baseUrl: baseUrl,
    apiKey: "YOUR_API_KEY",
    credentials: UserCredentials(
        token: "USER_TOKEN",
        secret: "USER_SECRET"
    )
)
```

## Activity Sources module
### Initialize ActivitySourcesManager

ActivitySourcesManager should be Initialize once as soon as possible after up app, for setup background delivery for the HealthKit to fetch intraday data, for example in AppDelegate.

``` swift
import FjuulActivitySources

class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {

        // Init apiClient with user credentials before init ActivitySourcesManager (see example in `Initialize of API Client`)
        ...

        let config = ActivitySourceConfigBuilder { builder in
            builder.healthKitConfig = HealthKitActivitySourceConfig(dataTypesToRead: [
                .activeEnergyBurned, .heartRate,
                .distanceCycling, .distanceWalkingRunning,
                .stepCount, .workout,
            ])
        }

        apiClient.initActivitySourcesManager(config: config)
    }
}
```

After call `apiClient.initActivitySourcesManager`, the `ActivitySourceManager` will be available by the call `apiClient.activitySourcesManager`.

### Collect HealthKit data

HealthKitActivitySource after connect will register background delivery observers. Then HealthKit will trigger the app each time when a new data type (types from HealthKitActivitySourceConfig) will be stored in HealthKit.

### Connect ActivitySource

``` swift
import FjuulActivitySources

apiCleint.activitySourcesManager?.connect(activitySource: HealthKitActivitySource.shared) { result in
    switch result {
    case .success(let connectionResult):
        switch connectionResult {
        case .connected:

          // After connect or disconnect always call ActivitySourcesManager.refreshCurrent() for mount new and unmount locally new and removed ActivitySource's.
          apiCleint.activitySourcesManager?.refreshCurrent { result in
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

For external trackers like Polar, Garmin, Suunto, the ConnectionResult will have `authenticationUrl` that need to be open in browser, for allow user go through auth flow. At the end of Auth flow, the app will be triggered by the deep link. To handle deep link there is prepared class `ExternalAuthenticationFlowHandler`:

``` swift
import FjuulActivitySources

let connectionStatus = ExternalAuthenticationFlowHandler.handle(url: url)
if connectionStatus.status {
    // Update activitySource list
    apiCleint.activitySourcesManager?.refreshCurrent { result in
        switch result {
        case .success(let connections):
            // Array of current ActivitySourceConnection's
        case .failure(let err):
            // handle error
        }
    }
}
```

## Analytics module
### Getting DailyStats

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