import Foundation

struct HKRequestData: Encodable {

    var caloriesData: [BatchDataPoint]?
    var cyclingData: [BatchDataPoint]?
    var stepsData: [BatchDataPoint]?
    var walkingData: [BatchDataPoint]?

    var empty: Bool {
        guard caloriesData != nil else { return true }
        guard cyclingData != nil else { return true }
        guard stepsData != nil else { return true }
        guard walkingData != nil else { return true }

        return false
    }
}
