import Foundation

public struct DailyStats: Codable {

    public let date: Date
    public let totalKcal: Float
    public let activeKcal: Float
    public let steps: UInt

    public let lowest: ActivityMeasure
    public let low: ActivityMeasure
    public let moderate: ActivityMeasure
    public let high: ActivityMeasure

}
