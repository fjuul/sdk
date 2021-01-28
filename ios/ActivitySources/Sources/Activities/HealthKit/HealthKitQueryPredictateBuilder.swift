import Foundation
import HealthKit

/// Class for build Healthkit query predicates base on ActivitySourceHKConfig and dirty batches.
class HealthKitQueryPredictateBuilder {
    let healthKitConfig: ActivitySourceHKConfig

    var wasUserEnteredPredicate: NSPredicate {
        NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)
    }

    init(healthKitConfig: ActivitySourceHKConfig) {
        self.healthKitConfig = healthKitConfig
    }

    func samplePredicate() -> NSCompoundPredicate {
        // Default predicate for prevent sync more that 30 days back
        let fromDate = Calendar.current.date(byAdding: .day, value: -30, to: Date())
        let startDatePredicate = HKQuery.predicateForSamples(withStart: fromDate, end: Date(), options: .strictStartDate)

        var predicates = [startDatePredicate]

        if !healthKitConfig.syncUserEnteredData {
            predicates.append(wasUserEnteredPredicate)
        }

        return NSCompoundPredicate(andPredicateWithSubpredicates: predicates)
    }
    
    /// Build predicate for HKStatisticsCollectionQuery based on ActivitySourceHKConfig and dirty batches
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
}
