import Foundation
import HealthKit

class DailyMetricFetcher {

    static var interval: DateComponents {
        var interval = DateComponents()
        interval.day = 1
        return interval
    }

    /// Fetch latest know value from HealthKit. If no anchor use HKSampleQuery for fetch only 1 last value, otherwise use HKAnchoredObjectQuery.
    /// - Parameters:
    ///   - type: HK sample type
    ///   - anchor: HKQueryAnchor
    ///   - completion: optional array of HKDailyMetricDataPoint and new anchor
    static func fetch(type: HKQuantityType, anchor: HKQueryAnchor?, predicateBuilder: HealthKitQueryPredicateBuilder,
                      completion: @escaping ([HKDailyMetricDataPoint]?, HKQueryAnchor?) -> Void) {
        self.dirtyDays(sampleType: type, anchor: anchor, predicate: predicateBuilder.samplePredicate()) { dirtyDays, newAnchor in
            if dirtyDays.isEmpty {
                completion(nil, newAnchor)
                return
            }
            let predicate = predicateBuilder.dailyMetricsCollectionsPredicate(days: dirtyDays)
            if type == HKObjectType.quantityType(forIdentifier: .stepCount) {
                self.getDailySteps(sampleType: type, predicate: predicate) { result in
                    completion(result, newAnchor)
                }
            }
            // TODO handle rhr
        }
    }

    private static func getDailySteps(sampleType: HKQuantityType, predicate: NSCompoundPredicate, completion: @escaping ([HKDailyMetricDataPoint]?) -> Void) {
        let query = HKStatisticsCollectionQuery(
            quantityType: sampleType,
            quantitySamplePredicate: predicate,
            options: .cumulativeSum,
            anchorDate: DateUtils.startOfDay(date: Date()),
            intervalComponents: interval
        )
        query.initialResultsHandler = { query, results, error in
            guard let stats = results?.statistics() else {
                completion(nil)
                return
            }
            if stats.isEmpty {
                // return nil instead of empty array in case of no data to not trigger a network
                // request with an empty array upload
                completion(nil)
                return
            }
            let dataPoints = stats.map { statistic in
                return HKDailyMetricDataPoint(
                    date: DateUtils.startOfDay(date: statistic.startDate),
                    steps: Int(statistic.sumQuantity()?.doubleValue(for: HKUnit.count()) ?? 0)
                )
            }
            completion(dataPoints)
        }
        HealthKitManager.healthStore.execute(query)
    }

    /// Detect dirty days by making an HKAnchoredObjectQuery and fetching dates from the new entries in HK.
    /// - Parameters:
    ///   - sampleType: HK sample type
    ///   - anchor: instance of HK anchor
    ///   - predicate: base predicate based on SDK config
    ///   - completion: Set of dirty days and new HK anchor
    private static func dirtyDays(sampleType: HKSampleType, anchor: HKQueryAnchor?, predicate: NSCompoundPredicate, completion: @escaping (Set<Date>, HKQueryAnchor?) -> Void) {
        var days: Set<Date> = []

        let query = HKAnchoredObjectQuery(type: sampleType,
                                          predicate: predicate,
                                          anchor: anchor,
                                          limit: HKObjectQueryNoLimit) { (_, samples, _, newAnchor, _) in
            guard let samples = samples else {
                completion(days, newAnchor)
                return
            }

            for sampleItem in samples {
                days.insert(DateUtils.startOfDay(date: sampleItem.startDate))
                days.insert(DateUtils.startOfDay(date: sampleItem.endDate))
            }

            completion(days, newAnchor)
        }
        HealthKitManager.healthStore.execute(query)
    }

}
