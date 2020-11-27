import Foundation
import HealthKit

struct HKRequestData: Encodable {
    var caloriesData: [BatchDataPoint]?
    var cyclingData: [BatchDataPoint]?
    var stepsData: [BatchDataPoint]?
    var walkingData: [BatchDataPoint]?
    var hrData: [HrBatchDataPoint]?
    var workouts: [WorkoutDataPoint]?
}

extension HKRequestData {
    static func build(sampleType: HKQuantityType, batches: [BatchDataPoint]) -> HKRequestData {
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: .activeEnergyBurned):
           return HKRequestData(caloriesData: batches)
        case HKObjectType.quantityType(forIdentifier: .stepCount):
            return HKRequestData(stepsData: batches)
        case HKObjectType.quantityType(forIdentifier: .distanceCycling):
            return HKRequestData(cyclingData: batches)
        case HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning):
            return HKRequestData(walkingData: batches)
        default:
            return HKRequestData()
        }
    }

    static func build(sampleType: HKQuantityType, batches: [HrBatchDataPoint]) -> HKRequestData {
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: .heartRate):
           return HKRequestData(hrData: batches)
        default:
            return HKRequestData()
        }
    }
}
