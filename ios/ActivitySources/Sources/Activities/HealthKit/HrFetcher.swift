import Foundation
import HealthKit

class HrFetcher {
    static func fetchIntradayData(sampleType: HKQuantityType, predicate: NSCompoundPredicate, batchDates: Set<Date>, completion: @escaping ([BatchDataPoint]) -> Void) {
        let calendar = Calendar.current
        var interval = DateComponents()
        interval.minute = 1

        // Always start from beginning of hour
        let anchorDate = HKDataUtils.beginningOfHour(date: calendar.date(byAdding: .day, value: -30, to: Date()))!

        let query = HKStatisticsCollectionQuery(quantityType: sampleType,
                                                quantitySamplePredicate: predicate,
                                                options: [.cumulativeSum, .separateBySource],
                                                anchorDate: anchorDate,
                                                intervalComponents: interval)
        // Set the results handler
        query.initialResultsHandler = { query, results, error in
            guard let statsCollection = results else {
                completion([])
                return
            }

            var result: [HKStatistics] = []
            var batches: [BatchDataPoint] = []

            statsCollection.enumerateStatistics(from: anchorDate, to: Date()) { statistics, _ in
                if statistics.sumQuantity() != nil {
                    result.append(statistics)
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

    static func fetch(predicate: NSCompoundPredicate, batchDates: Set<Date>, completion: @escaping ([HrBatchDataPoint]) -> Void) {
        let calendar = Calendar.current
        var interval = DateComponents()
        interval.minute = 1

        // Always start from beginning of hour
        let anchorDate = HKDataUtils.beginningOfHour(date: calendar.date(byAdding: .day, value: -30, to: Date()))!

        let query = HKStatisticsCollectionQuery(quantityType: HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.heartRate)!,
                                                quantitySamplePredicate: predicate,
                                                options: [.discreteMax, .discreteMin, .discreteAverage, .separateBySource],
                                                anchorDate: anchorDate,
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

            statsCollection.enumerateStatistics(from: anchorDate, to: Date()) { statistics, _ in
                if statistics.averageQuantity() != nil {
                    result.append(statistics)
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
    
    private static func unit(sampleType: HKQuantityType) -> HKUnit {
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!:
          return HKUnit.kilocalorie()
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceCycling)!,
             HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceWalkingRunning)!:
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
