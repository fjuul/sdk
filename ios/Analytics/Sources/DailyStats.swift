import Foundation

public struct DailyStats: Decodable {

    public let date: Date
    public let activeCalories: Float

    public let lowest: ActivityMeasure
    public let low: ActivityMeasure
    public let moderate: ActivityMeasure
    public let high: ActivityMeasure

}
