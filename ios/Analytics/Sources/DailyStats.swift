import Foundation

public struct DailyStats: Codable {

    public let date: Date
    public let activeKcal: Float
    public let bmr: Float

    public let low: ActivityMeasure
    public let moderate: ActivityMeasure
    public let high: ActivityMeasure

}
