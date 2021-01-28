import Foundation

/// The ActivitySource class for the Garmin tracker. This is an external activity source.
public final class ActivitySourceGarmin: ActivitySource {
    static public let shared = ActivitySourceGarmin()

    public var trackerValue = TrackerValue.GARMIN

    private init() {}
}
