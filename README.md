# Fjuul SDK

The Fjuul SDK is a proprietary SDK whose main tasks:

1. manage the state of the user's body parameters;
2. collect daily activities and related user metrics;
3. process and provide the aggregated results based on the received data from the user for a period of time;

# Architecture design of the SDK

The Fjuul SDK is presented as a set of modules, each of which is delimited by its own responsibilities. At the moment, they are as follows:

- **Core** - the root module that configures the behavior of all the others as well shares the common things between them. You can use it to initialize the SDK with the API credentials and set the user on whose behalf it operates.
- **User** - the module for working with users of the Fjuul SDK. For example, you can use it to create a user, get the latest state of the user profile, and update its properties.
- **Activity Sources** - the module responsible for setting up activity sources for the user, as well as collecting activity data from local sources.
- **Analytics** -  the module for accessing already aggregated statistics from the collected data sources.


# Fjuul Android SDK

Modules of Fjuul SDK for Android platform written on plain Java, so there should not be problems with integrating to your java project. Also, the project follows the [interoperability rules](https://developer.android.com/kotlin/interop#java_for_kotlin_consumption) for working with Kotlin lang (our ExampleApp which shows a usage of the SDK is written in Kotlin).

### Restrictions
- Fjuul SDK for Android runs on devices with Android 5.0 Lollipop (API level 21) or above. So, the SDK requires `minSdkVersion` to be set to 21 or higher.
- Fjuul SDK for Android uses Java 8+ API. Explore an official [guide](https://developer.android.com/studio/write/java8-support)
about java 8 desugaring if you plan to support older api levels (Android Gradle Plugin 4.0+).
- 'activitysources' module requires `google-play-services` on an Android device to work with the Google Fit API. This means that devices without the support of Google Play services (for example, phones manufactured by Huawei) wouldn't be able to connect to Google Fit.

### Distibution
Fjuul Android SDK modules are published to the private repository at Github Packages registry with the url `https://maven.pkg.github.com/fjuul/sdk`. In order to access Fjuul SDK modules you need to:

1. get the authentication token with 'read:packages' access to fjuul/sdk repository on Github;
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
In your `app/build.grade`:

- **Analytics**:

```implementation 'com.fjuul.sdk:analytics:+'```

- **Activity Sources**:

```com.fjuul.sdk:activitysources:+```

- **User**:

```com.fjuul.sdk:user:+```

### Getting started
Please follow the [link](docs/android-examples.md) to see examples of working with the Fjuul Android SDK API.

You can also refer to [ExampleApp](android/ExampleApp) which built for demonstration purposes.


# Fjuul iOS SDK

Modules of Fjuul SDK for iOS platform library written in Swift.

### Restrictions

- Fjuul SDK for iOS runs on devices with iOS 10+
- Fjuul SDK for iOS requires Xcode 11.4+ and Swift 5.2+


### Distibution

Fjuul iOS SDK packages are published at Github repo with the URL `https://github.com/fjuul/sdk`. In order to access Fjuul SDK modules you need to:

1. In XCode from menu, select `File > Swift Packages > Add Package Dependency`.
2. Fill package repository URL `https://github.com/fjuul/sdk`
3. Choose required package products: `FjuulCore, FjuulUser, FjuulAnalytics, FjuulActivitySources`


### Getting started
Please follow the [link](docs/ios-examples.md) to see examples of working with the Fjuul iOS SDK API.

You can also refer to [ExampleApp](ios/ExampleApp) which built for demonstration purposes.
