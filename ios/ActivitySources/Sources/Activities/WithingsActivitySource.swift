import Foundation

/// The ActivitySource class for the Withings tracker. This is an external activity source.
public final class WithingsActivitySource: ActivitySource {
    static public let shared = WithingsActivitySource()

    public var trackerValue = TrackerValue.WITHINGS

    private init() {}
}
