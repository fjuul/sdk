import Foundation

/// The ActivitySource class for the Strava tracker. This is an external activity source.
public final class StravaActivitySource: ActivitySource {
    static public let shared = StravaActivitySource()

    public var trackerValue = TrackerValue.STRAVA

    private init() {}
}
