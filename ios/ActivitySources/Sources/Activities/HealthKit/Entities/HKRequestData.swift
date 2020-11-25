import Foundation
import HealthKit

struct HKRequestData: Encodable {
    var caloriesData: [BatchDataPoint]?
    var cyclingData: [BatchDataPoint]?
    var stepsData: [BatchDataPoint]?
    var walkingData: [BatchDataPoint]?
    var hrData: [HrBatchDataPoint]?
}


extension HKRequestData {
    static func build(sampleType: HKQuantityType, batches: [BatchDataPoint]) -> HKRequestData {
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: .activeEnergyBurned)!:
           return HKRequestData(caloriesData: batches)
        case HKObjectType.quantityType(forIdentifier: .stepCount)!:
            return HKRequestData(stepsData: batches)
        case HKObjectType.quantityType(forIdentifier: .distanceCycling)!:
            return HKRequestData(cyclingData: batches)
        case HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning)!:
            return HKRequestData(walkingData: batches)
        default:
            return HKRequestData()
        }
    }
}
