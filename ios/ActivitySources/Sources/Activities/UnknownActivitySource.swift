import Foundation

/// The ActivitySource class for the Unknown tracker.
public final class UnknownActivitySource: ActivitySource {
    public var trackerValue: TrackerValue

    init(tracker: String) {
        self.trackerValue = TrackerValue(value: tracker)
    }
}
