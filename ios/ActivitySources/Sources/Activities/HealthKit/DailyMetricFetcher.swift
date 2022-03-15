import Foundation
import HealthKit

class DailyMetricFetcher {

    private static var interval: DateComponents {
        var interval = DateComponents()
        interval.day = 1
        return interval
    }

    private static var stepsMapper: (HKStatistics) -> HKDailyMetricDataPoint = { stat in
        return HKDailyMetricDataPoint(
            date: DateUtils.startOfDay(date: stat.startDate),
            steps: (stat.sumQuantity()?.doubleValue(for: HKUnit.count())).map { Int($0) }
        )
    }

    private static var rhrUnit: HKUnit {
        return HKUnit.count().unitDivided(by: HKUnit.minute())
    }

    private static var rhrMapper: (HKStatistics) -> HKDailyMetricDataPoint = { stat in
        return HKDailyMetricDataPoint(
            date: DateUtils.startOfDay(date: stat.startDate),
            restingHeartRate: (stat.averageQuantity()?.doubleValue(for: rhrUnit)).map { Int($0) }
        )
    }

    /// Fetch daily aggregate values from HealthKit for a given sample type
    /// - Parameters:
    ///   - type: HK sample type. Must be one of HealthKitConfigType.dailyTypes
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
                self.retrieveDailyAggregates(sampleType: type, predicate: predicate, options: .cumulativeSum, statisticMapper: stepsMapper) { result in
                    completion(result, newAnchor)
                }
                return
            }
            if type == HKObjectType.quantityType(forIdentifier: .restingHeartRate) {
                self.retrieveDailyAggregates(sampleType: type, predicate: predicate, options: .discreteAverage, statisticMapper: rhrMapper) { result in
                    completion(result, newAnchor)
                }
                return
            }
            DataLogger.shared.error("Unhandled daily metric type encountered: \(type)")
            completion(nil, newAnchor)
        }
    }

    private static func retrieveDailyAggregates(
        sampleType: HKQuantityType,
        predicate: NSCompoundPredicate,
        options: HKStatisticsOptions,
        statisticMapper: @escaping (HKStatistics) -> HKDailyMetricDataPoint,
        completion: @escaping ([HKDailyMetricDataPoint]?) -> Void
    ) {

        let query = HKStatisticsCollectionQuery(
            quantityType: sampleType,
            quantitySamplePredicate: predicate,
            options: options,
            anchorDate: DateUtils.startOfDay(date: Date()),
            intervalComponents: interval
        )
        query.initialResultsHandler = { _, results, _ in
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
            let dataPoints = stats.map(statisticMapper)
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
