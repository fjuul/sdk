# Android Examples

## Getting started with initialization
In order to use Fjuul SDK API you need to initialize `ApiClient` from **Core** module:

``` kotlin
import com.fjuul.sdk.core.ApiClient
...
val client = ApiClient.Builder(appContext, "YOUR_BASE_URL","YOUR_API_KEY").build()
```
This was initialization without user credentials, which is considered valid only for a few basic operations (for example, creating a user).  
If you plan to perform an action authorized by some user, you must provide credentials:
``` kotlin
import com.fjuul.sdk.core.ApiClient
import com.fjuul.sdk.core.entities.UserCredentials
...
val signedClient = ApiClient.Builder(appContext, "YOUR_BASE_URL", "YOUR_API_KEY")
            .setUserCredentials(UserCredentials("USER_TOKEN", "USER_SECRET"))
            .build()
```
Don't worry about how to get the user credentials, it will be described soon.

### Keeping the same reference of ApiClient
It's highly recommended to reuse the same instance of the once initialized client for all places. For that, you can implement a static singleton that will store the ref to the last initialized `ApiClient`. When you need to change something in the setup (for example, `UserCredentials`), just re-create the global instance of `ApiClient`.

## HTTP Requests
The SDK provides `ApiCall` to perform an HTTP request to the server. `ApiСall` has the ability to make a request in both asynchronous mode (`enqueue`) and synchronous mode (`execute`), blocking the thread in which it was called.  
For simplicity, the examples often use synchronous query execution.

## User module
`UserService` - the main class for working with users.

### Create a user
With basic `ApiClient` initialization, you can create a user: 
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
The result of the creation is an instance of `UserCreationResult` class which is a composition of the user profile and secret of the user. You should save the token and secret of the user.  
As mentioned earlier, to perform user-authorized actions, you must provide the user credentials to the `ApiClient.Builder`. The user credentials is a pair of token and secret:
```kotlin
// reinitialize Fjuul api-client with user credentials for signing all user authorized HTTP requests
val signedClient = ApiClient.Builder(appContext,
    "YOUR_BASE_URL",
    "YOUR_API_KEY")
    .setUserCredentials(UserCredentials(userProfile.token, userSecret))
    .build()
```

## Activity Sources module
`ActivitySourcesManager` is a high-level and main entity of **AcivitySources** module.  
It designed as the singleton, and before you will start using it, you must initialize it:
```kotlin
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
...
ActivitySourcesManager.initialize(signedClient)
```
The call above initializes `ActivitySourcesManager` with the default config.  
Consider using the overloaded method that receives the config as a parameter, if you need to adjust the behavior:
```kotlin
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManagerConfig
...
val config = ActivitySourcesManagerConfig.Builder()
    .setCollectableFitnessMetrics(setOf(FitnessMetricsType.INTRADAY_STEPS, FitnessMetricsType.INTRADAY_CALORIES))
    .enableGFBackgroundSync(Duration.ofMinutes(3))
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

### Connect to Google Fit
Request permissions:
```kotlin
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
...
val sourcesManager = ActivitySourcesManager.getInstance()
// connect to GoogleFit tracker
sourcesManager.connect(GoogleFitActivitySource.getInstance()) { connectResult ->
    val connectIntent = connectResult.value
    // this will show a window prompting GF permissions
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
val gfConnectionSource = sourcesManager.current?.find { connection -> connection.activitySource is GoogleFitActivitySource }
if (gfConnectionSource == null) {
    return;
}
```
Sync intraday data:
```kotlin
val gfActivitySource = gfConnectionSource.activitySource as GoogleFitActivitySource;
val syncOptions = GFIntradaySyncOptions.Builder().apply {
    setDateRange(LocalDate.now().minusDays(7), LocalDate.now())
    include(GFIntradaySyncOptions.METRICS_TYPE.CALORIES)
    include(GFIntradaySyncOptions.METRICS_TYPE.HEART_RATE)
    include(GFIntradaySyncOptions.METRICS_TYPE.STEPS)
}.build()
gfActivitySource.syncIntradayMetrics(syncOptions) { result ->
    if (result.isError) {
        // handle error
    }
}
```
Sync sessions data:
```kotlin
val gfActivitySource = gfConnectionSource.activitySource as GoogleFitActivitySource;
val syncOptions = GFSessionSyncOptions.Builder()
    .setDateRange(LocalDate.now().minusDays(7), LocalDate.now())
    .setMinimumSessionDuration(minSessionDuration)
    .build()
gfActivitySource.syncSessions(syncOptions) { result ->
    if (result.isError) {
        // handle error
    }
```

### Collect Google Fit data in the background
The SDK has the ability to sync Google Fit data in the background when all conditions are met:

1. the corresponding options must be enabled in the `ActivitySourcesManagerConfig` configuration (they are enabled in the default configuration):
```kotlin
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManagerConfig

val config = ActivitySourcesManagerConfig.Builder()
    .setCollectableFitnessMetrics(setOf(FitnessMetricsType.INTRADAY_STEPS, FitnessMetricsType.INTRADAY_CALORIES))
    .enableGFIntradayBackgroundSync()
    .enableGFSessionsBackgroundSync(Duration.ofMinutes(3))
    .build()
// or the same:
//  val config = ActivitySourcesManagerConfig.Builder()
//    .setCollectableFitnessMetrics(setOf(FitnessMetricsType.INTRADAY_STEPS, FitnessMetricsType.INTRADAY_CALORIES))
//    .enableGFBackgroundSync(Duration.ofMinutes(3))
//    .build()
ActivitySourcesManager.initialize(client, config)
```
2. the user must have a current connection to Google Fit;
3. Android OS or its vendor modifications allow your application to run in the background and do not restrict its execution. You can refer to [dontkillmyapp.com](https://dontkillmyapp.com/)  for getting more details.

## Analytics module
### Getting DailyStats
```kotlin
import com.fjuul.sdk.analytics.http.services.AnalyticsService
...
val analyticsService = AnalyticsService(signedClient)
val getDailyStatsApiCall = analyticsService.getDailyStats("2020-10-03", "2020-10-20")
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
