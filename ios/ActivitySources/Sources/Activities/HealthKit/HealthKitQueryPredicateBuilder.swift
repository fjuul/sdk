import Foundation
import HealthKit

/// Class for build Healthkit query predicates base on HealthKitActivitySourceConfig and dirty batches.
class HealthKitQueryPredicateBuilder {
    let healthKitConfig: HealthKitActivitySourceConfig

    var wasUserEnteredPredicate: NSPredicate {
        NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)
    }

    init(healthKitConfig: HealthKitActivitySourceConfig) {
        self.healthKitConfig = healthKitConfig
    }

    func samplePredicate() -> NSCompoundPredicate {
        let startDatePredicate = HKQuery.predicateForSamples(withStart: self.dataСollectionStartAt(), end: Date(), options: .strictStartDate)

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

    /// Start date of data collection. Maximum is 30 days back.
    /// - Returns: Date
    private func dataСollectionStartAt() -> Date {
        let calendar = Calendar.current
        // Default date for prevent sync more than 30 days back
        let defaultDate = calendar.startOfDay(for: calendar.date(byAdding: .day, value: -30, to: Date())!)

        guard let syncDataFrom = self.healthKitConfig.syncDataFrom else { return defaultDate }

        return syncDataFrom > defaultDate ? syncDataFrom : defaultDate
    }
}
