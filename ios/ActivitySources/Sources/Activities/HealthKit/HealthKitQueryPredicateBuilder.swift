import Foundation
import HealthKit

/// Class for build Healthkit query predicates base on HealthKitActivitySourceConfig and dirty batches.
class HealthKitQueryPredicateBuilder {
    let healthKitConfig: HealthKitActivitySourceConfig
    let startDate: Date?
    let endDate: Date?

    var wasUserEnteredPredicate: NSPredicate {
        NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)
    }

    init(healthKitConfig: HealthKitActivitySourceConfig, startDate: Date? = nil, endDate: Date? = nil) {
        self.healthKitConfig = healthKitConfig
        self.startDate = startDate
        self.endDate = endDate
    }

    func samplePredicate() -> NSCompoundPredicate {
        let startDatePredicate = HKQuery.predicateForSamples(withStart: self.dataСollectionStartAt(), end: self.dataСollectionEndAt(), options: .strictStartDate)

        var predicates = [startDatePredicate]

        if !healthKitConfig.syncUserEnteredData {
            predicates.append(wasUserEnteredPredicate)
        }

        return NSCompoundPredicate(andPredicateWithSubpredicates: predicates)
    }

    /// Build predicate for HKStatisticsCollectionQuery based on HealthKitActivitySourceConfig and dirty batches
    /// - Parameter batchDates: list of dirty batches
    /// - Returns: instance of NSCompoundPredicate
    func statisticsCollectionsPredicate(batchDates: Set<Date>) -> NSCompoundPredicate {
        let datePredicates = NSCompoundPredicate(type: .or, subpredicates: self.statisticsCollectionsDatePredicates(batchDates: batchDates))

        if !healthKitConfig.syncUserEnteredData {
            return NSCompoundPredicate(type: .and, subpredicates: [datePredicates, wasUserEnteredPredicate])
        } else {
            return datePredicates
        }
    }

    private func statisticsCollectionsDatePredicates(batchDates: Set<Date>) -> [NSPredicate] {
        var predicates: [NSPredicate] = []

        batchDates.forEach { (date) in
            if let endDate = DateUtils.endOfHour(date: date) {
                predicates.append(HKQuery.predicateForSamples(withStart: date, end: endDate, options: .strictStartDate))
            }
        }

        return predicates
    }

    func dailyMetricsCollectionsPredicate(days: Set<Date>) -> NSCompoundPredicate {
        let datePredicates: [NSPredicate] = days.map { date in
            let startOfDay = DateUtils.startOfDay(date: date)
            let endOfDay = DateUtils.endOfDay(date: date)
            // note we could pass nil instead of endOfDay here, but that would lead to all days starting from the first
            // dirty day getting reuploaded entirely regardless if they have changed or not; this can be considered if
            // the current approach would cause issues in practice with data changes around midnight
            return HKQuery.predicateForSamples(withStart: startOfDay, end: endOfDay, options: .strictStartDate)
        }
        let datePredicate = NSCompoundPredicate(type: .or, subpredicates: datePredicates)
        if !healthKitConfig.syncUserEnteredData {
            return NSCompoundPredicate(type: .and, subpredicates: [datePredicate, wasUserEnteredPredicate])
        } else {
            return datePredicate
        }
    }

    /// Start date of data collection. Maximum is 30 days back.
    /// - Returns: Date
    internal func dataСollectionStartAt() -> Date {
        let calendar = Calendar.current
        var dates = [
            // Default date for prevent sync more than 30 days back
            calendar.startOfDay(for: calendar.date(byAdding: .day, value: -30, to: Date())!),
        ]

        if let startDate = self.startDate {
            dates.append(startDate)
        }

        if let syncDataFrom = self.healthKitConfig.syncDataFrom {
            dates.append(syncDataFrom)
        }

        return dates.max()!
    }

    /// End date of data collection.
    /// - Returns: Date
    internal func dataСollectionEndAt() -> Date {
        guard let endDate = self.endDate else { return Date() }

        return [self.dataСollectionStartAt(), endDate].max()!
    }
}
