import Foundation

/// The ActivitySource class for the Suunto tracker. This is an external activity source.
public final class ActivitySourceSuunto: ActivitySource {
    static public let shared = ActivitySourceSuunto()

    public var trackerValue = TrackerValue.SUUNTO

    private init() {}
}
