import Foundation

public struct DailyStats: Codable {

    public let date: Date
    public let steps: Int

    public let low: ActivityMeasure
    public let moderate: ActivityMeasure
    public let high: ActivityMeasure

    public let contributingSources: [String]

}
