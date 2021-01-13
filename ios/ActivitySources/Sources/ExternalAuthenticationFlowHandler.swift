import Foundation

public struct ConnectionStatus {
    public var tracker: ActivitySourcesItem?
    public var success: Bool
}

/// Handler for the result of connecting to external activity sources.
/// Before calling the call ExternalAuthenticationFlowHandler.handle function, you should check that the schema of the incoming intent matches the expected for Fjuul SDK.
final public class ExternalAuthenticationFlowHandler {
    /// Determines the status of connecting to the external activity source and returns ConnectionStatus
    /// - Parameter url: URL
    /// - Returns: ConnectionStatus
    public static func handle(url: URL) -> ConnectionStatus {
        guard
            let components = URLComponents(url: url, resolvingAgainstBaseURL: true)
        else {
            return ConnectionStatus(success: false)
        }

        let trackerValue = components.queryItems?.first(where: { $0.name == "service" })?.value ?? "unknown"
        let tracker = ActivitySourcesItem(rawValue: trackerValue)

        let successValue = components.queryItems?.first(where: { $0.name == "success" })?.value ?? "false"
        let success = successValue == "true"

        return ConnectionStatus(tracker: tracker, success: success)
    }
}
