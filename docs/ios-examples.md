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
    profile[\.weight] = Decimal(string: "60.6")
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

### Mark user for deletion
The deletion of a user's data can be requested using:<br/>
```swift
import FjuulUser

// Init apiClient with user credentials before initializing ActivitySourcesManager (see example in `Initialize ApiClient`)
...

apiClient.user.markUserForDeletion { result in
    switch result {
    case .success:
        print("Successfully marked user for deletion.")
    case .failure(let err):
        print("Error: \(err)")
    }
}
```
After a successful request, the user won't be able to make any authorized requests. The user's data will be hard deleted within the constraints of the data protection agreement with the tenant.

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
                .activeEnergyBurned, .distanceCycling, .distanceWalkingRunning,
                .height, .weight
            ])
        }

        apiClient.initActivitySourcesManager(config: config) { result in
            switch result {
            case .success:
                print("Successfully restored local state.")
            case .failure(let err):
                print("Error: \(err)")
            }
        }
    }
}
```

After calling `apiClient.initActivitySourcesManager`, the `ActivitySourceManager` instance will be available as `apiClient.activitySourcesManager`.

### Collect HealthKit Data

HealthKitActivitySource will register background delivery observers after connecting. HealthKit will then trigger the app each time when new data (corresponding to the types from HealthKitActivitySourceConfig) was stored in HealthKit.
User-entered Apple Health data can be excluded by setting `syncUserEnteredData` to `false` in `HealthKitActivitySourceConfig` initialization. This will constrain fetched data based on the `HKMetadataKeyWasUserEntered` metadata key.

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
if connectionStatus.success {
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

Sync the user profile from HealthKit:
```swift
import FjuulActivitySources

guard let activitySource = activitySourceConnection.activitySource as? HealthKitActivitySource else {
    return
}

activitySource.syncProfile(configTypes: HealthKitConfigType.userProfileTypes) { result in
    switch result {
    case .success:
        print("Success sync")
    case .failure(let err): self.error = err
    }
}
```

Sync intraday data from HealthKit:
``` swift
import FjuulActivitySources

guard let activitySource = activitySourceConnection.activitySource as? HealthKitActivitySource else {
    return
}

activitySource.syncIntradayMetrics(startDate: self.fromDate, endDate: self.toDate, configTypes: HealthKitConfigType.intradayTypes) { result in
    switch result {
    case .success:
        print("Success sync")
    case .failure(let err): self.error = err
    }
}
```

Sync daily metric data from HealthKit:
``` swift
import FjuulActivitySources

guard let activitySource = activitySourceConnection.activitySource as? HealthKitActivitySource else {
    return
}

activitySource.syncDailyMetrics(startDate: self.fromDate, endDate: self.toDate, configTypes: HealthKitConfigType.dailyTypes) { result in
    switch result {
    case .success:
        print("Success sync")
    case .failure(let err): self.error = err
    }
}
```


Sync workouts data from HealthKit:
``` swift
import FjuulActivitySources

guard let activitySource = activitySourceConnection.activitySource as? HealthKitActivitySource else {
    return
}

activitySource.syncWorkouts(startDate: self.fromDate, endDate: self.toDate) { result in
    switch result {
    case .success:
        print("Success sync")
    case .failure(let err): self.error = err
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

### Get sum or average aggregates of DailyStats per time interval

``` swift
import FjuulAnalytics

let fromDate = Calendar.current.date(byAdding: .day, value: -7, to: Date())
let toDate = Date()

apiClient.analytics.aggregatedDailyStats(from: fromDate, to: toDate, aggregation: AggregationType.average) { result in
    switch result {
    case .success(let statsAvgs):
        // returns an object of daily stats' averages for requested period
        //{
        //    "activeKcal": 300,
        //    "bmr": 1700,
        //    "steps": 8600,
        //    "low": { "seconds": 1800, "metMinutes": 20 },
        //    "moderate": { "seconds": 1200, "metMinutes": 10 },
        //    "high": { "seconds": 180, "metMinutes": 15 },
        //}
    case .failure(let err):
        // handle error
    }
}
```
