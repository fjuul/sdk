import Foundation

/// The ActivitySource class for the Polar tracker. This is an external activity source.
public final class PolarActivitySource: ActivitySource {
    static public let shared = PolarActivitySource()

    public var trackerValue = TrackerValue.POLAR

    private init() {}
}
