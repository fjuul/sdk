import UIKit
import SwiftUI
import FjuulActivitySources

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    private var pendingConnectionCallback: URL?

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        // Use this method to optionally configure and attach the UIWindow to the provided UIWindowScene `scene`.
        // If using a storyboard, the `window` property will automatically be initialized and attached to the scene.
        // This delegate does not imply the connecting scene or session are new
        // (see `application:configurationForConnectingSceneSession` instead).

        // Create the SwiftUI view that provides the window contents.
        let contentView = RootView()
            .environmentObject(UserDefaultsManager.shared)
            .environmentObject(SessionStore.shared)

        // Use a UIHostingController as window root view controller.
        if let windowScene = scene as? UIWindowScene {
            let window = UIWindow(windowScene: windowScene)
            window.rootViewController = UIHostingController(rootView: contentView)
            self.window = window
            window.makeKeyAndVisible()
            pendingConnectionCallback = connectionOptions.urlContexts.first?.url
        }
    }

    //  Deeplink Handling for Scene based app
    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        guard let url = URLContexts.first?.url else {
            return
        }

        handleConnectionCallback(url: url)
    }

    private func handleConnectionCallback(url: URL) {
        guard url.scheme == "fjuulsdk-exampleapp",
              url.host == "external_connect" || url.host == "externalConnection" else { return }

        let connectionStatus = ExternalAuthenticationFlowHandler.handle(url: url)
        if connectionStatus.success {
            // While signing in or out there is nothing to refresh; a new session loads its connections itself.
            if let session = SessionStore.shared.session, SessionStore.shared.isActive(session.activitySourcesManager) {
                session.activitySources.getCurrentConnections()
            }
            return
        }

        let title: String
        let message: String
        switch connectionStatus.errorCode {
        case ExternalAuthenticationFlowHandler.ErrorCode.oauthCancelled:
            title = "Connection Cancelled"
            message = "The provider cancelled the connection. You can try again."
        case ExternalAuthenticationFlowHandler.ErrorCode.googleHealthAccountNotLinked:
            title = "Google Health Account Required"
            message = "Create a Google Health profile or migrate your Fitbit account, then return and retry the connection."
        default:
            title = "Connection Failed"
            message = "The tracker could not be connected. Please try again."
        }

        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        if connectionStatus.errorCode == ExternalAuthenticationFlowHandler.ErrorCode.googleHealthAccountNotLinked {
            alert.addAction(UIAlertAction(title: "Account Setup", style: .default) { _ in
                guard let signupURL = URL(string: "https://fitbit.google.com/auth/signup") else { return }
                UIApplication.shared.open(signupURL)
            })
        }
        alert.addAction(UIAlertAction(title: "OK", style: .cancel))
        window?.rootViewController?.present(alert, animated: true)
    }

    func sceneDidDisconnect(_ scene: UIScene) {
        // Called as the scene is being released by the system.
        // This occurs shortly after the scene enters the background, or when its session is discarded.
        // Release any resources associated with this scene that can be re-created the next time the scene connects.
        // The scene may re-connect later, as its session was not neccessarily discarded
        // (see `application:didDiscardSceneSessions` instead).
    }

    func sceneDidBecomeActive(_ scene: UIScene) {
        if let url = pendingConnectionCallback {
            pendingConnectionCallback = nil
            handleConnectionCallback(url: url)
        }
        // Called when the scene has moved from an inactive state to an active state.
        // Use this method to restart any tasks that were paused (or not yet started) when the scene was inactive.
    }

    func sceneWillResignActive(_ scene: UIScene) {
        // Called when the scene will move from an active state to an inactive state.
        // This may occur due to temporary interruptions (ex. an incoming phone call).
    }

    func sceneWillEnterForeground(_ scene: UIScene) {
        // Called as the scene transitions from the background to the foreground.
        // Use this method to undo the changes made on entering the background.
    }

    func sceneDidEnterBackground(_ scene: UIScene) {
        // Called as the scene transitions from the foreground to the background.
        // Use this method to save data, release shared resources, and store enough scene-specific state information
        // to restore the scene back to its current state.
    }

}
