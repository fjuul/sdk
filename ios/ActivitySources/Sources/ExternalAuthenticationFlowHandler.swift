import Foundation

/// Status of external connection from the deeplink handler
public struct ConnectionStatus {
    public var tracker: TrackerValue?
    public let success: Bool
    /// Optional server classification for a failed connection; unknown codes are preserved.
    public let errorCode: String?

    public init(tracker: TrackerValue? = nil, success: Bool) {
        self.init(tracker: tracker, success: success, errorCode: nil)
    }

    public init(tracker: TrackerValue? = nil, success: Bool, errorCode: String?) {
        self.tracker = tracker
        self.success = success
        self.errorCode = !success && errorCode != "" ? errorCode : nil
    }
}

/**
 Handler for the result of connecting to external activity sources.
 Before calling the call ExternalAuthenticationFlowHandler.handle function, you should check that the schema that incoming URL matches the expected for Fjuul SDK.
 Deeplinks https://developer.apple.com/documentation/xcode/allowing_apps_and_websites_to_link_to_your_content?language=objc

 ~~~
 //  Deeplink Handling for Scene based app
 func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
     guard let url = URLContexts.first?.url else {
         return
     }

     let connectionStatus = ExternalAuthenticationFlowHandler.handle(url: url)
     if connectionStatus.tracker != nil {
         // Update activitySource list
         activitySourceObserver.getCurrentConnections()
     }
 }
 ~~~
*/
final public class ExternalAuthenticationFlowHandler {
    /// Known server classifications. Compare these constants with ConnectionStatus.errorCode.
    public enum ErrorCode {
        public static let oauthCancelled = "oauth_cancelled"
        public static let googleHealthAccountNotLinked = "google_health_account_not_linked"
    }

    /// Determines the status of connecting to the external activity source and returns ConnectionStatus
    /// - Parameter url: instance of URL
    /// - Returns: ConnectionStatus
    public static func handle(url: URL) -> ConnectionStatus {
        guard
            let components = URLComponents(url: url, resolvingAgainstBaseURL: true)
        else {
            return ConnectionStatus(success: false)
        }

        let trackerName = components.queryItems?.first(where: { $0.name == "service" })?.value ?? "unknown"
        let tracker = TrackerValue(value: trackerName)

        let successValue = components.queryItems?.first(where: { $0.name == "success" })?.value ?? "false"
        let success = successValue == "true"
        let errorCode = components.queryItems?.first(where: { $0.name == "errorCode" })?.value

        return ConnectionStatus(tracker: tracker, success: success, errorCode: errorCode)
    }
}
