import Foundation
import HealthKit

struct HKRequestData: Encodable {
    var caloriesData: [BatchDataPoint]?
    var cyclingData: [BatchDataPoint]?
    var stepsData: [BatchDataPoint]?
    var walkingData: [BatchDataPoint]?
    var hrData: [HrBatchDataPoint]?
    var workoutsData: [WorkoutDataPoint]?
}

extension HKRequestData {
    static func build(quantityType: HKQuantityType, batches: [BatchDataPoint]) -> HKRequestData? {
        guard batches.count > 0 else {
            return nil
        }

        switch quantityType {
        case HKObjectType.quantityType(forIdentifier: .activeEnergyBurned):
           return HKRequestData(caloriesData: batches)
        case HKObjectType.quantityType(forIdentifier: .stepCount):
            return HKRequestData(stepsData: batches)
        case HKObjectType.quantityType(forIdentifier: .distanceCycling):
            return HKRequestData(cyclingData: batches)
        case HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning):
            return HKRequestData(walkingData: batches)
        default:
            return nil
        }
    }

    static func build(quantityType: HKQuantityType, batches: [HrBatchDataPoint]) -> HKRequestData? {
        guard batches.count > 0 else {
            return nil
        }

        switch quantityType {
        case HKObjectType.quantityType(forIdentifier: .heartRate):
           return HKRequestData(hrData: batches)
        default:
            return nil
        }
    }
}
