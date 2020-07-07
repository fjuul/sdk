import Foundation

public struct DailyStats: Decodable {

    let date: Date
    let activeCalories: Float

    let lowest: ActivityMeasure
    let low: ActivityMeasure
    let moderate: ActivityMeasure
    let high: ActivityMeasure

}
