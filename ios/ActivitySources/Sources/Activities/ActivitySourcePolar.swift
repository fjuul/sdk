import Foundation

/// The ActivitySource class for the Polar tracker. This is an external activity source.
public final class ActivitySourcePolar: ActivitySource {
    static public let shared = ActivitySourcePolar()

    public var trackerValue = TrackerValue.POLAR

    private init() {}
}
