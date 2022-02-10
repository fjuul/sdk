import Foundation

public struct AggregatedDailyStats: Codable {

    public let activeKcal: Float
    public let bmr: Float
    public let steps: Int

    public let low: ActivityMeasure
    public let moderate: ActivityMeasure
    public let high: ActivityMeasure

}
