import Foundation

/// The ActivitySource class for the Oura tracker. This is an external activity source.
public final class OuraActivitySource: ActivitySource {
    static public let shared = OuraActivitySource()

    public var trackerValue = TrackerValue.OURA

    private init() {}
}
