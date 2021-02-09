import Foundation
import HealthKit

/// Data structure for aggregated values
struct HKRequestData: Encodable {
    var caloriesData: [BatchDataPoint]?
    var cyclingData: [BatchDataPoint]?
    var stepsData: [BatchDataPoint]?
    var walkingData: [BatchDataPoint]?
    var hrData: [HrBatchDataPoint]?
    var workoutsData: [WorkoutDataPoint]?
}

extension HKRequestData {
    /// Builds HKRequestData from list of BatchDataPoint
    /// - Parameters:
    ///   - quantityType: Healthkit HKQuantityType
    ///   - batches: collections of BatchDataPoint's
    /// - Returns: instance of HKRequestData or nil
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

    /// Builds HKRequestData from list of HrBatchDataPoint
    /// - Parameters:
    ///   - batches: collections of HrBatchDataPoint's
    /// - Returns: instance of HKRequestData or nil
    static func build(batches: [HrBatchDataPoint]) -> HKRequestData? {
        guard batches.count > 0 else {
            return nil
        }

        return HKRequestData(hrData: batches)
    }
}
