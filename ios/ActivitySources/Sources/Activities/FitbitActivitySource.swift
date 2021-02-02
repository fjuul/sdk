import Foundation

/// The ActivitySource class for the Fitbit tracker. This is an external activity source.
public final class FitbitActivitySource: ActivitySource {
    static public let shared = FitbitActivitySource()

    public var trackerValue = TrackerValue.FITBIT

    private init() {}
}
