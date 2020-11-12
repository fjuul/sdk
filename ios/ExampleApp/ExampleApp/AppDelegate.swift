import UIKit
import FjuulCore
import FjuulActivitySources

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let environment = ApiEnvironment(rawValue: UserDefaults.standard.integer(forKey: "environment"))!

        // TODO: Update code for initialize SDK client in example app
        ApiClientHolder.default.apiClient = ApiClient(
            baseUrl: environment.baseUrl,
            apiKey: UserDefaults.standard.string(forKey: "apiKey") ?? "",
            credentials: UserCredentials(
                token: UserDefaults.standard.string(forKey: "token") ?? "",
                secret: UserDefaults.standard.string(forKey: "secret") ?? ""
            )
        )

        if let apiClient = ApiClientHolder.default.apiClient {
            ActivitySourceManager.shared.initialize(apiClient: apiClient)
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

    // MARK: Tracker Connection Deeplink Handling

    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        // Note: handling this is entirely optional, as the connection is already successfully established at this point
        // (unless the user has cancelled the process and this indicates an unsuccessful connection - however this logic
        // here has no effect on the outcome of the connection, and there is no guarantee the user will return to the app
        // through the deeplink).
        let connectionStatus = ExternalAuthenticationFlowHandler.handle(url: url)
        if let tracker = connectionStatus.tracker {
            print("returned from connecting to: \(tracker) with status: \(connectionStatus.success)")
            return connectionStatus.success
        }
        return false
    }
}
