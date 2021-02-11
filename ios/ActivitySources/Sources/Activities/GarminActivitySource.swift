import Foundation

/// The ActivitySource class for the Garmin tracker. This is an external activity source.
public final class GarminActivitySource: ActivitySource {
    static public let shared = GarminActivitySource()

    public var trackerValue = TrackerValue.GARMIN

    private init() {}
}
