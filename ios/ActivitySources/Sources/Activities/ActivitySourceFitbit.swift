import Foundation

/// The ActivitySource class for the Fitbit tracker. This is an external activity source.
public final class ActivitySourceFitbit: ActivitySource {
    static public let shared = ActivitySourceFitbit()

    public var trackerValue = TrackerValue.FITBIT

    private init() {}
}
