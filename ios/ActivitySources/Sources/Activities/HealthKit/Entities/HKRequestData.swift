import Foundation

struct HKRequestData: Encodable {
    var caloriesData: [BatchDataPoint]?
    var cyclingData: [BatchDataPoint]?
    var stepsData: [BatchDataPoint]?
    var walkingData: [BatchDataPoint]?
    var hrData: [HrBatchDataPoint]?
}
