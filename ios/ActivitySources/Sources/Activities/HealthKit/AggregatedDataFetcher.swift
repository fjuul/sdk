import Foundation
import HealthKit

class AggregatedDataFetcher {
    static var interval: DateComponents {
        var interval = DateComponents()
        interval.minute = 1

        return interval
    }

    static func fetch(type: HKQuantityType, anchor: HKQueryAnchor,
                      predictateBuilder: HealthKitQueryPredictateBuilder, completion: @escaping (HKRequestData?, HKQueryAnchor?) -> Void) {

        let sampleType = type as HKSampleType

        self.dirtyBatches(sampleType: sampleType, anchor: anchor, predicate: predictateBuilder.samplePredicate()) { batchDates, newAnchor in

            let predicate = predictateBuilder.statisticsCollectionsPredicate(batchDates: batchDates)

            if type == HKObjectType.quantityType(forIdentifier: .heartRate) {
                self.fetchHrData(predicate: predicate, batchDates: batchDates) { results in

                    let hkRequestData = HKRequestData.build(quantityType: type, batches: results)

                    completion(hkRequestData, newAnchor)
                }
            } else {
                self.fetchIntradayData(sampleType: type, predicate: predicate, batchDates: batchDates) { results in

                    let hkRequestData = HKRequestData.build(quantityType: type, batches: results)

                    completion(hkRequestData, newAnchor)
                }
            }
        }
    }

    private static func dirtyBatches(sampleType: HKSampleType, anchor: HKQueryAnchor, predicate: NSCompoundPredicate, completion: @escaping (Set<Date>, HKQueryAnchor?) -> Void) {
        var batchStartDates: Set<Date> = []

        let query = HKAnchoredObjectQuery(type: sampleType,
                                          predicate: predicate,
                                          anchor: anchor,
                                          limit: HKObjectQueryNoLimit) { (_, samples, _, newAnchor, _) in
            guard let samples = samples else {
                completion(batchStartDates, newAnchor)
                return
            }

            for sampleItem in samples {
                if let date = HKDataUtils.beginningOfHour(date: sampleItem.startDate) {
                    batchStartDates.insert(date)
                }
            }

            completion(batchStartDates, newAnchor)
        }
        HealthKitManager.healthStore.execute(query)
    }

    static private func fetchIntradayData(sampleType: HKQuantityType, predicate: NSCompoundPredicate, batchDates: Set<Date>, completion: @escaping ([BatchDataPoint]) -> Void) {
        let query = HKStatisticsCollectionQuery(quantityType: sampleType,
                                                quantitySamplePredicate: predicate,
                                                options: [.cumulativeSum, .separateBySource],
                                                anchorDate: self.statisticsCollectionAnchor(),
                                                intervalComponents: interval)
        // Set the results handler
        query.initialResultsHandler = { query, results, error in
            guard let statsCollection = results else {
                completion([])
                return
            }

            var result: [HKStatistics] = []
            var batches: [BatchDataPoint] = []

            batchDates.forEach { batchStart in
                let batchEnd = Calendar.current.date(byAdding: .hour, value: 1, to: batchStart)!

                statsCollection.enumerateStatistics(from: batchStart, to: batchEnd) { statistics, _ in
                    if statistics.sumQuantity() != nil {
                        result.append(statistics)
                    }
                }
            }

            self.groupByHour(data: result).forEach { (_, entries: [HKStatistics]) in
                let uniqueSources = Array(Set(entries.compactMap { $0.sources }.flatMap { $0 }.map { $0.bundleIdentifier }))
                let items = entries.map { statistics -> AggregatedDataPoint? in
                    if let quantity = statistics.sumQuantity() {
                        let value = quantity.doubleValue(for: self.unit(sampleType: sampleType))
                        return AggregatedDataPoint(value: value, start: statistics.startDate)
                    }

                    return nil
                }.compactMap { $0 }

                batches.append(BatchDataPoint(sourceBundleIdentifiers: uniqueSources, entries: items))
            }

            completion(batches)
        }

        HealthKitManager.healthStore.execute(query)
    }

    static private func fetchHrData(predicate: NSCompoundPredicate, batchDates: Set<Date>, completion: @escaping ([HrBatchDataPoint]) -> Void) {
        let query = HKStatisticsCollectionQuery(quantityType: HKObjectType.quantityType(forIdentifier: .heartRate)!,
                                                quantitySamplePredicate: predicate,
                                                options: [.discreteMax, .discreteMin, .discreteAverage, .separateBySource],
                                                anchorDate: self.statisticsCollectionAnchor(),
                                                intervalComponents: interval)
        // Set the results handler
        query.initialResultsHandler = { query, results, error in
            guard let statsCollection = results else {
                completion([])
                return
            }

            var result: [HKStatistics] = []
            var batches: [HrBatchDataPoint] = []
            let unit = HKUnit.count().unitDivided(by: HKUnit.minute())

            batchDates.forEach { batchStart in
                let batchEnd = Calendar.current.date(byAdding: .hour, value: 1, to: batchStart)!

                statsCollection.enumerateStatistics(from: batchStart, to: batchEnd) { statistics, _ in
                    if statistics.averageQuantity() != nil {
                        result.append(statistics)
                    }
                }
            }

            self.groupByHour(data: result).forEach { (_, entries: [HKStatistics]) in
                let uniqueSources = Array(Set(entries.compactMap { $0.sources }.flatMap { $0 }.map { $0.bundleIdentifier }))

                let items = entries.map { statistics -> HeartRateDataPoint? in
                    if let avgQuantity = statistics.averageQuantity(), let maxQuantity = statistics.maximumQuantity(), let minQuantity = statistics.minimumQuantity() {
                        return HeartRateDataPoint(
                            start: statistics.startDate,
                            avg: avgQuantity.doubleValue(for: unit),
                            min: minQuantity.doubleValue(for: unit),
                            max: maxQuantity.doubleValue(for: unit)
                        )
                    }

                    return nil
                }.compactMap { $0 }

                batches.append(HrBatchDataPoint(sourceBundleIdentifiers: uniqueSources, entries: items))
            }

            completion(batches)
        }

        HealthKitManager.healthStore.execute(query)
    }

    // With interval 1 min, it doesn’t matter what date will be set, just need starts from beginning of minute
    // Technically, the anchor sets the start time for a single time interval.
    private static func statisticsCollectionAnchor() -> Date {
        return Calendar.current.date(bySettingHour: 12, minute: 0, second: 0, of: Date())!
    }

    private static func unit(sampleType: HKQuantityType) -> HKUnit {
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: .activeEnergyBurned):
          return HKUnit.kilocalorie()
        case HKObjectType.quantityType(forIdentifier: .distanceCycling),
             HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning):
            return HKUnit.meter()
        default:
          return HKUnit.count()
        }
    }

    static private func groupByHour(data: [HKStatistics]) -> [Date: [HKStatistics]] {
        let initial: [Date: [HKStatistics]] = [:]

        let groupedByDateComponents = data.reduce(into: initial) { acc, cur in
            let components = Calendar.current.dateComponents([.year, .month, .day, .hour], from: cur.startDate)
            let date = Calendar.current.date(from: components)!
            let existing = acc[date] ?? []
            acc[date] = existing + [cur]
        }

        return groupedByDateComponents
    }
}