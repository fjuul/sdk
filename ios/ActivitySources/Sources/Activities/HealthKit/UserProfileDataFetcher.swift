import Foundation
import HealthKit
import FjuulCore

/// Fetch height and weight data from HK
class UserProfileDataFetcher {
    /// Fetch latest know value from HealthKit. If no anchor use HKSampleQuery for fetch only 1 last value, otherwise use HKAnchoredObjectQuery
    /// - Parameters:
    ///   - type: sample type (bodyData or heaight)
    ///   - anchor: HKQueryAnchor
    ///   - completion: HKUserProfileData and new anchor
    static func fetch(type: HKQuantityType, anchor: HKQueryAnchor?, completion: @escaping (HKUserProfileData?, HKQueryAnchor?) -> Void) {

        if let anchor = anchor {
            self.fetchByAnchoredObjectQuery(type: type, anchor: anchor, predicate: nil) { data, newAnchor in
                completion(data, newAnchor)
            }
        } else {
            // When no saved anchor, query HealthKit with HKSampleQuery that supports order and limit
            self.fetchLatestSample(type: type) { sample in
                guard let sample = sample else {
                    // no data in HealthKit
                    completion(nil, nil)
                    return
                }

                let predicate = HKQuery.predicateForSamples(withStart: sample.startDate, end: nil)
                self.fetchByAnchoredObjectQuery(type: type, anchor: nil, predicate: predicate) { data, newAnchor in
                    completion(data, newAnchor)
                }
            }
        }
    }

    /// Fetch latest Sample from HealthKit by query with anchor
    /// - Parameters:
    ///   - type: HKSampleType
    ///   - anchor: anchor
    ///   - predicate: NSPredicate
    ///   - completion: HKUserProfileData and new anchor
    private static func fetchByAnchoredObjectQuery(type: HKQuantityType, anchor: HKQueryAnchor?, predicate: NSPredicate?,
                                                   completion: @escaping (HKUserProfileData?, HKQueryAnchor?) -> Void) {
        let query = HKAnchoredObjectQuery(type: type,
                                          predicate: predicate,
                                          anchor: anchor,
                                          limit: HKObjectQueryNoLimit) { (_, samples, _, newAnchor, _) in
            // Getting the last known value
            guard let result = samples?.last as? HKQuantitySample,
                  let unit = try? self.unit(sampleType: type) else {

                // no data
                completion(nil, newAnchor)
                return
            }

            let value = result.quantity.doubleValue(for: unit)

            if type == HKObjectType.quantityType(forIdentifier: .bodyMass) {
                completion(HKUserProfileData(weight: value), newAnchor)
            } else {
                completion(HKUserProfileData(height: value), newAnchor)
            }
        }
        HealthKitManager.healthStore.execute(query)
    }

    /// Fetch latest Sample from HealthKit
    /// - Parameters:
    ///   - type: HKSampleType
    ///   - completion: optional HKQuantitySample
    private static func fetchLatestSample(type: HKQuantityType, completion: @escaping (HKQuantitySample?) -> Void) {
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: false)

        let query = HKSampleQuery(sampleType: type, predicate: nil, limit: 1, sortDescriptors: [sortDescriptor]) { (_, results, _) in
                if let result = results?.first as? HKQuantitySample {
                    completion(result)
                    return
                }

                //no data
                completion(nil)
            }

        HealthKitManager.healthStore.execute(query)
    }

    /// Get healthkit QuantityType value unit
    /// - Parameter sampleType: healthkit quantityType
    /// - Returns: HKUnit
    private static func unit(sampleType: HKQuantityType) throws -> HKUnit {
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: .bodyMass):
          return HKUnit.gramUnit(with: .kilo)
        case HKObjectType.quantityType(forIdentifier: .height):
            return HKUnit.meterUnit(with: .centi)
        default:
            throw FjuulError.activitySourceFailure(reason: .wrongHealthKitObjectType)
        }
    }
}
