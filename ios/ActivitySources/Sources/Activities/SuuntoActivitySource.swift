import Foundation

/// The ActivitySource class for the Suunto tracker. This is an external activity source.
public final class SuuntoActivitySource: ActivitySource {
    static public let shared = SuuntoActivitySource()

    public var trackerValue = TrackerValue.SUUNTO

    private init() {}
}
