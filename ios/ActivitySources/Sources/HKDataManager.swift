#if canImport(HealthKit)
import Foundation
import HealthKit
import FjuulCore

class HKDataManager {
    internal let healthKitStore: HKHealthStore = HKHealthStore()
    internal var hkAnchorStore: HKAnchorStore

    init(persistor: Persistor) {
        self.hkAnchorStore = HKAnchorStore(persistor: persistor)
    }

    var readableTypes: Set<HKSampleType> {
        return samplesReadableTypes.union(intradayReadableTypes)
    }
    var intradayReadableTypes: Set<HKQuantityType> {
        // TODO: types should be based on config
        return [
            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!,
//            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!,
//            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceCycling)!,
//            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceWalkingRunning)!
        ]
    }
    var samplesReadableTypes: Set<HKSampleType> {
        // TODO: types should be based on config
        return [
            HKWorkoutType.workoutType(),
//            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.heartRate)!,
        ]
    }
    func authorizeHealthKitAccess(_ completion: ((_ success: Bool, _ error: Error?) -> Void)!) {
        guard HKHealthStore.isHealthDataAvailable() else {
            completion(false, FjuulError.activitySourceFailure(reason: .hkNotAvailableOnDevice))
            return
        }

        healthKitStore.requestAuthorization(toShare: nil, read: readableTypes) { (success, error) in
            completion(success, error)
        }
    }
    func observe() {
        for sampleType in self.intradayReadableTypes {
            let query: HKObserverQuery = HKObserverQuery(sampleType: sampleType, predicate: nil, updateHandler: { _, completionHandler, error in
                defer { completionHandler() }
                guard error != nil else { return }

                self.fetchIntradayUpdates(sampleType: sampleType)
            })
            healthKitStore.execute(query)
            healthKitStore.enableBackgroundDelivery(for: sampleType, frequency: .immediate, withCompletion: {(succeeded: Bool, error: Error?) in
               if succeeded {
                    print("Enabled background delivery for type \(sampleType)")
                    self.fetchIntradayUpdates(sampleType: sampleType)
               } else {
                // TODO: Add return error
//                   if let theError = error {
//                   }
               }
           })
        }
        for sampleType in self.samplesReadableTypes {
            let query: HKObserverQuery = HKObserverQuery(sampleType: sampleType, predicate: nil, updateHandler: { _, completionHandler, error in
                defer { completionHandler() }
                guard error == nil else { return }

                self.fetchSampleUpdates(sampleType: sampleType)
                
//                print("Emulate fetch activeEnergyBurned")
//                self.fetchIntradayUpdates(sampleType: HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!)
            })
            healthKitStore.execute(query)
            healthKitStore.enableBackgroundDelivery(for: sampleType, frequency: .immediate, withCompletion: {(succeeded: Bool, error: Error?) in
               if succeeded {
                   print("Enabled background delivery for type \(sampleType)")
               } else {
                   if let theError = error {
                       print("Failed to enable background delivery of weight changes. ")
                       print("Error = \(theError)")
                   }
               }
           })
        }
    }
    private func fetchSampleUpdates(sampleType: HKSampleType) {
        let anchorDate = self.hkAnchorStore.anchor?.activeEnergyBurned ?? HKQueryAnchor.init(fromValue: 0)

        // Exclude manually added data
        let wasUserEnteredPredicate = NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)
        let fromDate = Calendar.current.date(byAdding: .day, value: -30, to: Date())
        let startDatePredicate = HKQuery.predicateForSamples(withStart: fromDate, end: Date(), options: .strictStartDate)
        let compound = NSCompoundPredicate(andPredicateWithSubpredicates: [startDatePredicate, wasUserEnteredPredicate])

        let query = HKAnchoredObjectQuery(type: sampleType,
                                              predicate: compound,
                                              anchor: anchorDate,
                                              limit: HKObjectQueryNoLimit) { (_, samplesOrNil, deletedObjectsOrNil, newAnchor, errorOrNil) in
            guard let samples = samplesOrNil, let deletedObjects = deletedObjectsOrNil else {
                print("*** An error occurred during the initial query: \(errorOrNil!.localizedDescription) ***")
                return
            }
            // TODO: find batches for re-upload?
            for sampleItem in samples {
                print("Sample item: \(sampleItem)")
            }
//            // TODO: find batches for re-upload?
//            for deletedSampleItem in deletedObjects {
//                print("deleted: \(deletedSampleItem)")
//            }

//            self.hkAnchorStore.anchor?.activeEnergyBurned = newAnchor
        }
        healthKitStore.execute(query)
    }
    private func fetchIntradayUpdates(sampleType: HKQuantityType) {
        self.getBatchSegments(sampleType: sampleType) { (batchStartDates) in
            print("batchStartDates: \(batchStartDates)")
            self.fetchIntradayStatisticsCollections(sampleType: sampleType, batchDates: batchStartDates) { results in
                let batches = HKBatchAggregator(data: results).generate()
                print("got results, \(batches.count)")
            }
        }
    }
    private func getBatchSegments(sampleType: HKQuantityType, completion: @escaping (Set<Date>) -> Void) {
        var batchStartDates: Set<Date> = []

        let anchorDate = self.hkAnchorStore.anchor?.activeEnergyBurned ?? HKQueryAnchor.init(fromValue: 0)

        // Exclude manually added data
        let wasUserEnteredPredicate = NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)
        let fromDate = Calendar.current.date(byAdding: .day, value: -30, to: Date())
        let startDatePredicate = HKQuery.predicateForSamples(withStart: fromDate, end: Date(), options: .strictStartDate)
        let compound = NSCompoundPredicate(andPredicateWithSubpredicates: [startDatePredicate, wasUserEnteredPredicate])
        let query = HKAnchoredObjectQuery(type: sampleType,
                                              predicate: compound,
                                              anchor: anchorDate,
                                              limit: HKObjectQueryNoLimit) { (_, samplesOrNil, _, newAnchor, errorOrNil) in
            guard let samples = samplesOrNil else {
                // TODO: Perform proper error handling here
                print("*** An error occurred during the initial query: \(errorOrNil!.localizedDescription) ***")
                return
            }
            
            for sampleItem in samples {
                batchStartDates.insert(HKDataUtils.beginningOfHour(date: sampleItem.startDate)!)
            }

            self.hkAnchorStore.anchor?.activeEnergyBurned = newAnchor
            completion(batchStartDates)
        }
        healthKitStore.execute(query)
    }

    private func fetchIntradayStatisticsCollections(sampleType: HKQuantityType, batchDates: Set<Date>, completion: @escaping ([HKStatistics]) -> Void) {
        let calendar = Calendar.current
        var interval = DateComponents()
        interval.minute = 1

        // Exclude manually added data
//        let wasUserEnteredPredicate = NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)
        let datePredicates = self.statisticsCollectionsDatePredicates(batchDates: batchDates)

        let compound = NSCompoundPredicate(orPredicateWithSubpredicates: datePredicates)

        // TODO: Calculate correctly anchor
        let anchorDate = HKDataUtils.beginningOfHour(date: calendar.date(byAdding: .day, value: -30, to: Date()))!

        let query = HKStatisticsCollectionQuery(quantityType: sampleType,
                                                quantitySamplePredicate: compound,
                                                options: [.cumulativeSum, .separateBySource],
                                                anchorDate: anchorDate,
                                                intervalComponents: interval)
        // Set the results handler
        query.initialResultsHandler = { query, results, error in
            guard let statsCollection = results else {
                // TODO: Perform proper error handling here
                completion([])
                return
            }

            var result: [HKStatistics] = []
            let endDate = Date()

            // TODO: Iterate based on batches, for performance improvement
            statsCollection.enumerateStatistics(from: anchorDate, to: endDate) { statistics, _ in
                if statistics.sumQuantity() != nil {
                    result.append(statistics)
                }
            }
            completion(result)
        }

        healthKitStore.execute(query)
    }

    private func statisticsCollectionsDatePredicates(batchDates: Set<Date>) -> [NSPredicate] {
        var predicates: [NSPredicate] = []

        batchDates.forEach { (date) in
            predicates.append(HKQuery.predicateForSamples(withStart: date, end: HKDataUtils.endOfHour(date: date)!, options: .strictStartDate))
        }

        return predicates
    }

//    private func observerCompletionHandler(query: HKObserverQuery!, completionHandler: HKObserverQueryCompletionHandler!, error: Error?) {
//        print("Changed data in Health App")
//        print(query.sampleType)
//
//        // TODO: figure out how get sampleType from query
//        self.fetchIntradayUpdates(sampleType: HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!)
//
//        completionHandler()
//    }
}
#endif
