Fjuul SDK, Android version.
When building the SDK from the command line, you must use JDK version 17.
Tested with: Temurin-17.0.8.1+1.

To test Google Health Connect implementation, in ActivitySourcesManager.java
uncomment two calls to addGoogleHealthConnectIfAbsent(...). These are
commented out so that unit tests pass.
