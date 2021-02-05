import Foundation

/// Status of external connection from the deeplink handler
public struct ConnectionStatus {
    public var tracker: TrackerValue?
    public var success: Bool
}

/**
 Handler for the result of connecting to external activity sources.
 Before calling the call ExternalAuthenticationFlowHandler.handle function, you should check that the schema that incoming matches the expected for Fjuul SDK.
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

        return ConnectionStatus(tracker: tracker, success: success)
    }
}
