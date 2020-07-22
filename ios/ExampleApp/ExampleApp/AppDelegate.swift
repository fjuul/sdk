import UIKit
import FjuulCore

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var apiClient: ApiClient?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let credentials = UserCredentials(
            token: "",
            secret: ""
        )
        self.apiClient = ApiClient(
            baseUrl: "",
            apiKey: "",
            credentials: credentials
        )
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
