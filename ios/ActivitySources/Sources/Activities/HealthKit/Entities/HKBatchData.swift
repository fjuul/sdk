import Foundation
import HealthKit

/// Data structure for aggregated values
struct HKBatchData: Encodable {
    var caloriesData: [BatchDataPoint]?
    var cyclingData: [BatchDataPoint]?
    var walkingData: [BatchDataPoint]?
    var hrData: [HrBatchDataPoint]?
    var workoutsData: [WorkoutDataPoint]?
}

extension HKBatchData {
    /// Builds HKBatchData from list of BatchDataPoint.
    /// - Parameters:
    ///   - quantityType: Healthkit HKQuantityType
    ///   - batches: collections of BatchDataPoint's
    /// - Returns: instance of HKBatchData or nil
    static func build(quantityType: HKQuantityType, batches: [BatchDataPoint]) -> HKBatchData? {
        guard batches.count > 0 else {
            return nil
        }

        switch quantityType {
        case HKObjectType.quantityType(forIdentifier: .activeEnergyBurned):
           return HKBatchData(caloriesData: batches)
        case HKObjectType.quantityType(forIdentifier: .distanceCycling):
            return HKBatchData(cyclingData: batches)
        case HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning):
            return HKBatchData(walkingData: batches)
        default:
            return nil
        }
    }

    /// Builds HKBatchData from list of HrBatchDataPoint.
    /// - Parameters:
    ///   - batches: collections of HrBatchDataPoint's
    /// - Returns: instance of HKBatchData or nil
    static func build(batches: [HrBatchDataPoint]) -> HKBatchData? {
        guard batches.count > 0 else {
            return nil
        }

        return HKBatchData(hrData: batches)
    }
}
