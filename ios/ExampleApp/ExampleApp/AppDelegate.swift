import UIKit
import FjuulCore
import FjuulActivitySources
import Logging

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {

        // Set default logger globally, any subsequent Logger instances created using the Logger(label:) initializer will
        // default to the specified handler.
        // Can be configured to choose any compatible logging backend implementation.
        // https://github.com/apple/swift-log
        LoggingSystem.bootstrap { label in
            var logger = StreamLogHandler.standardOutput(label: label)

            #if DEBUG
                logger.logLevel = .info
            #else
                logger.logLevel = .error
            #endif

            return logger
        }

        if let apiClient = FjuulApiBuilder.buildApiClient() {
            ApiClientHolder.default.apiClient = apiClient

            FjuulApiBuilder.buildActivitySourcesManager(apiClient: apiClient)
        }

        return true
    }

    // MARK: UISceneSession Lifecycle
    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }

    func application(_ application: UIApplication, didDiscardSceneSessions sceneSessions: Set<UISceneSession>) {
        // Called when the user discards a scene session.
        // If any sessions were discarded while the application was not running, this will be called shortly
        // after application:didFinishLaunchingWithOptions. Use this method to release any resources that
        // were specific to the discarded scenes, as they will not return.
    }
}
