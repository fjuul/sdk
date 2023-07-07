import Foundation

/// TrackerValue carries the raw string presentation of the tracker and contains predefined static constants for known
/// activity sources in terms of the current SDK version and platform.
public class TrackerValue: Equatable {
    public static let FITBIT = TrackerValue(value: "fitbit")
    public static let GARMIN = TrackerValue(value: "garmin")
    public static let OURA = TrackerValue(value: "oura")
    public static let POLAR = TrackerValue(value: "polar")
    public static let STRAVA = TrackerValue(value: "strava")
    public static let SUUNTO = TrackerValue(value: "suunto")
    public static let WITHINGS = TrackerValue(value: "withings")
    public static let HEALTHKIT = TrackerValue(value: "healthkit")

    static var constants = [ FITBIT, GARMIN, OURA, POLAR, STRAVA, SUUNTO, HEALTHKIT, WITHINGS ]

    public static func == (lhs: TrackerValue, rhs: TrackerValue) -> Bool {
        return lhs.value == rhs.value
    }

    static func forValue(value: String) -> TrackerValue? {
        return constants.first { item in item.value == value }
    }

    public let value: String

    init(value: String) {
        self.value = value
    }
}
