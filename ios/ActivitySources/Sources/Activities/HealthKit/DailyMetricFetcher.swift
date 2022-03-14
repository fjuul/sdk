import Foundation
import HealthKit

class DailyMetricFetcher {

    static var interval: DateComponents {
        var interval = DateComponents()
        interval.day = 1
        return interval
    }

    private static func statisticsCollectionAnchor() -> Date {
        return Calendar.current.date(bySettingHour: 12, minute: 0, second: 0, of: Date())!
    }

    /// Fetch latest know value from HealthKit. If no anchor use HKSampleQuery for fetch only 1 last value, otherwise use HKAnchoredObjectQuery.
    /// - Parameters:
    ///   - type:
    ///   - anchor: HKQueryAnchor
    ///   - completion:  and new anchor
    static func fetch(type: HKQuantityType, anchor: HKQueryAnchor?, predicateBuilder: HealthKitQueryPredicateBuilder,
                      completion: @escaping ([HKDailyMetricDataPoint], HKQueryAnchor?) -> Void) {
        self.dirtyDays(sampleType: type, anchor: anchor, predicate: predicateBuilder.samplePredicate()) { dirtyDays, newAnchor in
            let predicate = predicateBuilder.dailyMetricsCollectionsPredicate(days: dirtyDays)
            if type == HKObjectType.quantityType(forIdentifier: .stepCount) {
                self.getDailySteps(sampleType: type, predicate: predicate) { result in
                    completion(result, newAnchor)
                }
            }
        }
    }

    private static func getDailySteps(sampleType: HKQuantityType, predicate: NSCompoundPredicate, completion: @escaping ([HKDailyMetricDataPoint]) -> Void) {
        let query = HKStatisticsCollectionQuery(
            quantityType: sampleType,
            quantitySamplePredicate: predicate,
            options: .cumulativeSum,
            anchorDate: statisticsCollectionAnchor(),
            intervalComponents: interval
        )
        query.initialResultsHandler = { query, results, error in
            guard let stats = results else {
                completion([])
                return
            }
            let dataPoints = stats.statistics().map { statistic in
                return HKDailyMetricDataPoint(
                    date: statistic.startDate,
                    steps: statistic.sumQuantity()?.doubleValue(for: HKUnit.count())
                )
            }
            completion(dataPoints)
        }
        HealthKitManager.healthStore.execute(query)
    }

    /// Detect dirty days by making a HKAnchoredObjectQuery and fetching dates from the new entries in HK.
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
