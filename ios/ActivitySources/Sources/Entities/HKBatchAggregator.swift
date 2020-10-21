import Foundation
import HealthKit

class HKBatchAggregator {
    let data: [HKStatistics]

    init(data: [HKStatistics]) {
        self.data = data
    }

    func generate() -> [BatchDataPoint] {
        var batches: [BatchDataPoint] = []

        self.groupByHour().forEach { (date, entries: [HKStatistics]) in
            let uniqueSources = Array(Set(entries.compactMap { $0.sources }.flatMap { $0 }.map { $0.bundleIdentifier }))
            let items = entries.map { statistics -> AggregatedDataPoint? in
                if let quantity = statistics.sumQuantity() {
                    let value = quantity.doubleValue(for: HKUnit.kilocalorie())
                    return AggregatedDataPoint(value: value, startDate: statistics.startDate)
                }

                return nil
            }.compactMap { $0 }

            batches.append(BatchDataPoint(sources: uniqueSources, items: items))
        }

        return batches
    }

    private func groupByHour() -> [Date: [HKStatistics]] {
        let initial: [Date: [HKStatistics]] = [:]

        let groupedByDateComponents = self.data.reduce(into: initial) { acc, cur in
            let components = Calendar.current.dateComponents([.year, .month, .day, .hour], from: cur.startDate)
            let date = Calendar.current.date(from: components)!
            let existing = acc[date] ?? []
            acc[date] = existing + [cur]
        }

        return groupedByDateComponents
    }
}
