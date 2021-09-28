# Android Examples

## Getting Started with Initialization
In order to use the Fjuul SDK API you need to initialize an `ApiClient` from the **Core** module:

``` kotlin
import com.fjuul.sdk.core.ApiClient
...
val client = ApiClient.Builder(appContext, "YOUR_BASE_URL","YOUR_API_KEY").build()
```
This initialization without user credentials is considered valid only for a few basic operations (for example, creating a user).<br/>
If you plan to perform an action authorized by some user, you must provide credentials:
``` kotlin
import com.fjuul.sdk.core.ApiClient
import com.fjuul.sdk.core.entities.UserCredentials
...
val signedClient = ApiClient.Builder(appContext, "YOUR_BASE_URL", "YOUR_API_KEY")
            .setUserCredentials(UserCredentials("USER_TOKEN", "USER_SECRET"))
            .build()
```
Refer to the "[Create a User](#create-a-user)" section on instructions how to obtain user credentials.

### Keeping the same reference of ApiClient
It's highly recommended to initialize an `ApiClient` once and reuse the same instance throughout your codebase. For that, you can implement a static singleton that will store the reference to the last initialized `ApiClient`. When you need to change something in the setup (for example, `UserCredentials`), just re-create the global instance of `ApiClient`.

## HTTP Requests
The SDK provides `ApiCall` to perform HTTP requests to the server. `ApiСall` has the ability to make requests in both asynchronous mode (`enqueue`) and synchronous mode (`execute`), blocking the thread in which it was called.
For simplicity, the examples in this document use synchronous query execution.

## User Module
`UserService` - the main class for working with users.

### Create a User
With basic `ApiClient` initialization, you can create a user:<br/>
```kotlin
import com.fjuul.sdk.core.ApiClient

val client: ApiClient = ...
val userService = UserService(client)
val profileBuilder = UserProfile.PartialBuilder()
profileBuilder.apply {
    setHeight(170.5f)
    setWeight(72f)
    setGender(Gender.male)
    setBirthDate(LocalDate.parse("1990-10-01"))
}
val createUserApiCall = userService.createUser(profileBuilder)
val createUserApiCallResult = createUserApiCall.execute()
if (!createUserApiCallResult.isError) {
    throw createUserApiCallResult.error!!
}
val userCreationResult = createUserApiCallResult.value!!
val userProfile = userCreationResult.user
val userSecret = userCreationResult.secret
```
The result of the creation is an instance of `UserCreationResult`, which is a composition of the user profile and secret of the user. You should persist the token and secret of the user.<br/>
To perform user-authorized actions, you must provide the user credentials to the `ApiClient.Builder`. User credentials are a pair of token and secret:
```kotlin
// reinitialize Fjuul api-client with user credentials for signing all user authorized HTTP requests
val signedClient = ApiClient.Builder(appContext,
    "YOUR_BASE_URL",
    "YOUR_API_KEY")
    .setUserCredentials(UserCredentials(userProfile.token, userSecret))
    .build()
```

## Activity Sources Module
`ActivitySourcesManager` is the high-level and main entity of the **AcivitySources** module. It is designed as a singleton, which must be initialized before the first use:
```kotlin
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
...
ActivitySourcesManager.initialize(signedClient)
```
The call above initializes `ActivitySourcesManager` with the default config.<br/>
Consider using the overloaded method that receives the config as a parameter, if you need to adjust the behavior:
```kotlin
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManagerConfig
...
val config = ActivitySourcesManagerConfig.Builder()
    .setCollectableFitnessMetrics(setOf(FitnessMetricsType.INTRADAY_STEPS, FitnessMetricsType.INTRADAY_CALORIES))
    .enableGoogleFitBackgroundSync(Duration.ofMinutes(3))
    .build()
ActivitySourcesManager.initialize(signedClient, config)
```
`setCollectableFitnessMetrics` implies what permissions will be requested to local activity sources during the connection (e.g. Google Fit) and what kind of data will be collected in the background mode for current connections to local activity sources.

To retrieve an instance of `ActivitySourcesManager`:
```kotlin
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
...
val sourcesManager = ActivitySourcesManager.getInstance()
```

### Preface to Google Fit
Before working with the Google Fit activity source, you need to make the initial setup in the console of your project on Google Cloud Platform. Please follow the official [guide](https://developers.google.com/fit/android/get-api-key) for getting an OAuth 2.0 client ID with the enabled Fitness API.

### Connect to Google Fit
Request permissions:
```kotlin
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
...
val sourcesManager = ActivitySourcesManager.getInstance()
// connect to GoogleFit tracker
sourcesManager.connect(GoogleFitActivitySource.getInstance()) { connectResult ->
    val connectIntent = connectResult.value
    // this will show a window prompting Google Fit permissions
    startActivityForResult(connectIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
}
```
Next, to finish connecting to Google Fit , you need to handle an invocation of  `onActivityResult` in your Activity/Fragment:
```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
    GoogleFitActivitySource.getInstance().handleGoogleSignInResult(data) { result ->
        if (result.isError) {
            // error
        }
        // success, refresh current connections
    }
}
```

### Collect Google Fit data
Find out if the user has a current connection to Google Fit:
```kotlin
val sourcesManager = ActivitySourcesManager.getInstance()
val googleFitConnectionSource = sourcesManager.current?.find { connection -> connection.activitySource is GoogleFitActivitySource }
if (googleFitConnectionSource == null) {
    return;
}
```
Sync the user profile from Google Fit:
``` kotlin
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.GoogleFitProfileSyncOptions
...
val googleFitActivitySource = googleFitConnectionSource.activitySource as GoogleFitActivitySource;
val syncOptions = GoogleFitProfileSyncOptions.Builder()
    .include(FitnessMetricsType.HEIGHT)
    .include(FitnessMetricsType.WEIGHT)
    .build()
googleFitActivitySource.syncProfile(syncOptions) { result ->
    if (result.isError) {
        // handle error
    }
}
```

Sync intraday data:
```kotlin
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.entities.GoogleFitIntradaySyncOptions
...
val googleFitActivitySource = googleFitConnectionSource.activitySource as GoogleFitActivitySource;
val syncOptions = GoogleFitIntradaySyncOptions.Builder().apply {
    setDateRange(LocalDate.now().minusDays(7), LocalDate.now())
    include(FitnessMetricsType.INTRADAY_CALORIES)
    include(FitnessMetricsType.INTRADAY_HEART_RATE)
    include(FitnessMetricsType.INTRADAY_STEPS)
}.build()
googleFitActivitySource.syncIntradayMetrics(syncOptions) { result ->
    if (result.isError) {
        // handle error
    }
}
```
Sync sessions data:
```kotlin
import com.fjuul.sdk.activitysources.entities.GoogleFitSessionSyncOptions
...
val googleFitActivitySource = googleFitConnectionSource.activitySource as GoogleFitActivitySource;
val syncOptions = GoogleFitSessionSyncOptions.Builder()
    .setDateRange(LocalDate.now().minusDays(7), LocalDate.now())
    .setMinimumSessionDuration(minSessionDuration)
    .build()
googleFitActivitySource.syncSessions(syncOptions) { result ->
    if (result.isError) {
        // handle error
    }
}
```

### Collect Google Fit Data in the Background
The SDK has the ability to sync Google Fit data in the background when all of the following conditions are met:

1. the corresponding options must be enabled in the `ActivitySourcesManagerConfig` configuration (they are enabled in the default configuration):
```kotlin
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManagerConfig

val config = ActivitySourcesManagerConfig.Builder()
    .setCollectableFitnessMetrics(setOf(FitnessMetricsType.INTRADAY_STEPS, FitnessMetricsType.INTRADAY_CALORIES))
    .enableGoogleFitIntradayBackgroundSync()
    .enableGoogleFitSessionsBackgroundSync(Duration.ofMinutes(3))
    .enableProfileBackgroundSync()
    .build()
// or the same:
//  val config = ActivitySourcesManagerConfig.Builder()
//    .setCollectableFitnessMetrics(setOf(FitnessMetricsType.INTRADAY_STEPS, FitnessMetricsType.INTRADAY_CALORIES))
//    .enableGoogleFitBackgroundSync(Duration.ofMinutes(3))
//    .enableProfileBackgroundSync()
//    .build()
ActivitySourcesManager.initialize(client, config)
```
2. the user must have a current connection to Google Fit
3. Android OS or its vendor modifications allow your application to run in the background and do not restrict its execution. You can refer to [dontkillmyapp.com](https://dontkillmyapp.com/) for getting more details.

## Analytics Module
### Getting DailyStats
```kotlin
import com.fjuul.sdk.analytics.http.services.AnalyticsService
...
val analyticsService = AnalyticsService(signedClient)
val getDailyStatsApiCall = analyticsService.getDailyStats(LocalDate.parse("2020-10-03"), LocalDate.parse("2020-10-20"))
val getDailyStatsApiResult = getDailyStatsApiCall.execute()
if (getDailyStatsApiResult.isError) {
    // handle error
}
val dailyStats = getDailyStatsApiResult.value!!
dailyStats.forEach { item ->
    val formattedItem = """
        |date: ${item.date};
        |low: ${item.low.metMinutes} metMinutes;
        |moderate: ${item.moderate.metMinutes} metMinutes;
        |high: ${item.high.metMinutes} metMinutes""".trimMargin()
        println(formattedItem)
}
```

### Getting sum or average aggregates of DailyStats per time interval
```kotlin
import com.fjuul.sdk.analytics.http.services.AnalyticsService
...
val analyticsService = AnalyticsService(signedClient)
val getAggregatedDailyStatsApiCall = analyticsService.getAggregatedDailyStats(LocalDate.parse("2020-10-03"), LocalDate.parse.("2020-10-20"), AggregationType.sum)
val getAggregatedDailyStatsApiResult = getAggregatedDailyStatsApiCall.execute()
if (getAggregatedDailyStatsApiResult.isError) {
    // handle error
}
val statsSums = getDailyStatsApiResult.value!!
statsSums.forEach { item ->
    val formattedItem = """
        |low: ${item.low.metMinutes} metMinutes;
        |moderate: ${item.moderate.metMinutes} metMinutes;
        |high: ${item.high.metMinutes} metMinutes""".trimMargin()
        println(formattedItem)
}
```

## Logging
Fjuul SDK uses [Timber](https://github.com/JakeWharton/timber) for logging internal events and exposes this dependency to consumers.<br/>
For ordinary debugging, you can use `DebugTimberTree` that writes everything from Fjuul SDK to the standard android's Log:
``` kotlin
import com.fjuul.sdk.core.utils.DebugTimberTree
import timber.log.Timber

public class MainApplication {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTimberTree())
        }
    }
}
```
Also, you can extend this default debug implementation to filter only wanted entries:
```kotlin
import com.fjuul.sdk.core.utils.DebugTimberTree
import timber.log.Timber

Timber.plant(object: DebugTimberTree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return super.isLoggable(tag, priority) && priority >= Log.WARN
    }
})
```

### Change logs output
You are free to implement your own logger for Fjuul SDK by extending `TimberTree` and applying it to Timber:
``` kotlin
import com.fjuul.sdk.core.utils.TimberTree
import timber.log.Timber

Timber.plant(object: TimberTree() {
    override fun doLog(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Crashlytics.logEvent("$tag: $message")
    }
})
```
### Using Timber for your own logging
If you plan to use Timber in your application then you should exclude all incoming entries from Fjuul SDK by tag:

``` kotlin
import com.fjuul.sdk.core.utils.Logger
import timber.log.Timber

Timber.plant(object: Timber.DebugTree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return tag != Logger.TAG
    }
})
```
