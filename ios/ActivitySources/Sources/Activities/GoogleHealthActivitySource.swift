import Foundation

/// The ActivitySource class for the Google Health tracker. This is an external activity source.
public final class GoogleHealthActivitySource: ActivitySource {
    static public let shared = GoogleHealthActivitySource()

    public var trackerValue = TrackerValue.GOOGLEHEALTH

    private init() {}
}
