import Foundation

/// The ActivitySource class for the Unknown tracker.
public final class ActivitySourceUnknown: ActivitySource {
    public var trackerValue: TrackerValue

    init(tracker: String) {
        self.trackerValue = TrackerValue(value: tracker)
    }
}
