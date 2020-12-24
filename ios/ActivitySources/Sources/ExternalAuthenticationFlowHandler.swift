import Foundation

public struct ConnectionStatus {
    public var tracker: ActivitySourcesItem?
    public var success: Bool
}

final public class ExternalAuthenticationFlowHandler {
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
