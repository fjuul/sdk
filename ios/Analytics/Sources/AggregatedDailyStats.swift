import Foundation

public struct AggregatedDailyStats: Codable {

    public let steps: Int

    public let low: ActivityMeasure
    public let moderate: ActivityMeasure
    public let high: ActivityMeasure

    public let contributingSources: [String]

}
