# Fjuul SDK
This repository contains the Fjuul SDK for Android and iOS. Its primary capabilities are:

1. managing the state of a user's body parameters
2. managing connections to activity sources and collecting daily activities and related user metrics from on-device sources
3. providing the unified and aggregated health and fitness metrics of a user

## General Architecture
The Fjuul SDK is structured as a set of independent modules, each of which is delimited by its own responsibilities. At the moment, they are as follows:

- **Core** - the root module that configures the behavior of all the others and shares the common things between them. You can use it to initialize the SDK with the API credentials and set the user on whose behalf it operates.
- **User** - the module for working with users of the Fjuul SDK. For example, you can use it to create a user, get the latest state of the user profile, and update its properties.
- **Activity Sources** - the module responsible for setting up activity sources for the user, as well as collecting activity data from on-device sources.
- **Analytics** -  the module for accessing aggregated statistics from the collected data sources.


## Android
Fjuul SDK for Android is written in plain Java, so there shouldn't be any problems with integrating it to your Java project. In addition, the project follows the [interoperability rules](https://developer.android.com/kotlin/interop#java_for_kotlin_consumption) for working with Kotlin (the ExampleApp demonstrating the usage of the SDK is implemented in Kotlin).

### Restrictions
- Fjuul SDK for Android runs on devices with Android 5.0 Lollipop (API level 21) or above. So, the SDK requires `minSdkVersion` to be set to 21 or higher.
- Fjuul SDK for Android uses Java 8+ API. Explore the [official guide](https://developer.android.com/studio/write/java8-support)
about java 8 desugaring if you plan to support older api levels (Android Gradle Plugin 4.0+).
- The 'activitysources' module requires `google-play-services` on an Android device to work with the Google Fit API. This means that devices with missing Google Play services (for example, phones manufactured by Huawei) wouldn't be able to connect to Google Fit.

### Distibution
Fjuul SDK for Android is published to the private Github Packages registry with the url `https://maven.pkg.github.com/fjuul/sdk`. In order to access its modules you need to:

1. get an authentication token with 'read:packages' access to fjuul/sdk repository on GitHub
2. declare a maven repository in your `build.gradle` in the following way:
```groovy
allprojects {
    ...
    repositories {
        ...
        maven {
            name = 'GitHubPackages'
            url = uri('https://maven.pkg.github.com/fjuul/sdk')
            credentials {
                username = 'GITHUB_ACTOR'
                password = 'GITHUB_TOKEN'
            }
        }
    }
}
```

### Installation
Add SDK module dependencies as needed in your `app/build.grade`:
```groovy
dependencies {
    implementation 'com.fjuul.sdk:user:+'
    implementation 'com.fjuul.sdk:activitysources:+'
    implementation 'com.fjuul.sdk:analytics:+'
}
```

### Getting started
A collection of examples showcasing the usage of the SDK is available [here](docs/android-examples.md).

You can also refer to the [ExampleApp](android/ExampleApp) which demonstrates the integration and usage of the SDK.


## iOS
Fjuul SDK for iOS is written in Swift, and published as an SPM package.

### Restrictions
- Fjuul SDK for iOS runs on devices with iOS 10+
- Fjuul SDK for iOS requires Xcode 12.4+ and Swift 5.2+

### Distibution
Fjuul SDK for iOS is published in this repository. In order to access its modules you need to:

1. Select `File > Swift Packages > Add Package Dependency` from the main menu in XCode
2. Fill the package repository URL with either `https://github.com/fjuul/sdk` or `git@github.com:fjuul/sdk.git`, depending on your preferred method of authentication
3. Choose package products as needed: `FjuulCore, FjuulUser, FjuulActivitySources, FjuulAnalytics`

### Getting started
A collection of examples showcasing the usage of the SDK is available [here](docs/ios-examples.md).

You can also refer to the [ExampleApp](ios/ExampleApp) which demonstrates the integration and usage of the SDK.
