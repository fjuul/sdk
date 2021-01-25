import Foundation
import HealthKit

class HealthKitQueryPredictateBuilder {
    let healthKitConfig: ActivitySourceHKConfig

    var wasUserEnteredPredicate: NSPredicate {
        NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)
    }

    init(healthKitConfig: ActivitySourceHKConfig) {
        self.healthKitConfig = healthKitConfig
    }

    func samplePredicate() -> NSCompoundPredicate {
        let fromDate = Calendar.current.date(byAdding: .day, value: -30, to: Date())
        let startDatePredicate = HKQuery.predicateForSamples(withStart: fromDate, end: Date(), options: .strictStartDate)

        var predicates = [startDatePredicate]

        if !healthKitConfig.syncUserEnteredData {
            predicates.append(wasUserEnteredPredicate)
        }

        return NSCompoundPredicate(andPredicateWithSubpredicates: predicates)
    }

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
            if let endDate = HKDataUtils.endOfHour(date: date) {
                predicates.append(HKQuery.predicateForSamples(withStart: date, end: endDate, options: .strictStartDate))
            }
        }

        return predicates
    }
}
