import Foundation
import HealthKit

//let energyType: HKQuantityType = HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!
//let hrType: HKQuantityType = HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.heartRate)!
//let cyclingDistanceType: HKQuantityType = HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceCycling)!
//let runningDistanceType: HKQuantityType = HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceWalkingRunning)!
//let stepCountType: HKQuantityType = HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!

class ActivitySourceHealthKit {
    internal let healthKitStore: HKHealthStore = HKHealthStore()

    init() {}
    
    private var readableTypes: Set<HKSampleType> {
        return samplesReadableTypes.union(intradayReadableTypes)
    }
    
    private var intradayReadableTypes: Set<HKQuantityType> {
        // TODO: types should be based on config
        return [
            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!,
//            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!,
//            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceCycling)!,
//            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceWalkingRunning)!
        ]
    }
    
    private var samplesReadableTypes: Set<HKSampleType> {
        // TODO: types should be based on config
        return [
//            HKWorkoutType.workoutType(),
//            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.heartRate)!,
//            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceWalkingRunning)!,
//            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceCycling)!,
        ]
    }

    // TODO: Check with team, but probably that the best place for set activitySource on server side
    func mount() {
        guard HKHealthStore.isHealthDataAvailable() else {
            return
        }

        self.authorizeHealthKitAccess { (success, error) in
          if success {
            self.observe()
          } else {
            if error != nil {
              print("\(String(describing: error))")
            }
          }
        }
    }

    func unmount() {}
    func sync() {
        // TODO: figure out how get sampleType from query
        // TODO: sync types based on config
//        self.fetchIntradayUpdates(sampleType: HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!)
    }
    func mountBackgroundDelivery() {}
    func unmountBackgroundDelivery() {}

    func authorizeHealthKitAccess(_ completion: ((_ success:Bool, _ error:Error?) -> Void)!) {
        healthKitStore.requestAuthorization(toShare: nil, read: readableTypes) { (success, error) in
            if success {
                print("requestAuthorization: success")
            } else {
                print("requestAuthorization: failure")
            }

            completion(success, error)
        }
    }
    
    private func observe() {
        for sampleType in self.intradayReadableTypes {
            var query: HKObserverQuery = HKObserverQuery(sampleType: sampleType, predicate: nil, updateHandler: { query, completionHandler, error in
                defer { completionHandler() }
                guard error != nil else { return }

                self.fetchIntradayUpdates(sampleType: sampleType)
            })
            
            healthKitStore.execute(query)
            healthKitStore.enableBackgroundDelivery(for: sampleType, frequency: .immediate, withCompletion: {(succeeded: Bool, error: Error?) in
               if succeeded{
                    print("Enabled background delivery for type \(sampleType)")
                    self.fetchIntradayUpdates(sampleType: sampleType)
               } else {
                   if let theError = error{
                       print("Failed to enable background delivery of weight changes. ")
                       print("Error = \(theError)")
                   }
               }
           })
        }
        
        for sampleType in self.samplesReadableTypes {
            var query: HKObserverQuery = HKObserverQuery(sampleType: sampleType, predicate: nil, updateHandler: { query, completionHandler, error in
                defer { completionHandler() }
                guard error == nil else { return }

                self.fetchSampleUpdates(sampleType: sampleType)
            })
            
            healthKitStore.execute(query)
            healthKitStore.enableBackgroundDelivery(for: sampleType, frequency: .immediate, withCompletion: {(succeeded: Bool, error: Error?) in
               if succeeded{
                   print("Enabled background delivery for type \(sampleType)")
               } else {
                   if let theError = error{
                       print("Failed to enable background delivery of weight changes. ")
                       print("Error = \(theError)")
                   }
               }
           })
        }
    }

    private func observerCompletionHandler(query: HKObserverQuery!, completionHandler: HKObserverQueryCompletionHandler!, error: Error?) {
        print("Changed data in Health App")
        print(query.sampleType)
        
        // TODO: figure out how get sampleType from query
        self.fetchIntradayUpdates(sampleType: HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!)
        
        completionHandler()
    }
    
    private func fetchIntradayUpdates(sampleType: HKQuantityType) {
        
        let batchStartDates = self.getBatchSegments(sampleType: sampleType)
        self.fetchIntradayStatisticsCollections(sampleType: sampleType, batchDates: batchStartDates)
    }
    
    private func getBatchSegments(sampleType: HKQuantityType) -> Set<Date> {
        var BatchStartDates: Set<Date> = []
        let anchorKey = "\(sampleType)-Intrada"

        // TODO: Calculate right anchor from persisted store
        var anchorDate = HKQueryAnchor.init(fromValue: 0)
        if let data = UserDefaults.standard.object(forKey: anchorKey) as? Data {
            anchorDate = (NSKeyedUnarchiver.unarchiveObject(with: data) as? HKQueryAnchor)!
            print(anchorDate)
        }
        
        // Exclude manually added data
        let wasUserEnteredPredicate = NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)
        let fromDate = Calendar.current.date(byAdding: .day, value: -30, to: Date())
        let startDatePredicate = HKQuery.predicateForSamples(withStart: fromDate, end: Date(), options: .strictStartDate)
        let compound = NSCompoundPredicate(andPredicateWithSubpredicates: [startDatePredicate, wasUserEnteredPredicate])
        
        let query = HKAnchoredObjectQuery(type: sampleType,
                                              predicate: compound,
                                              anchor: anchorDate,
                                              limit: HKObjectQueryNoLimit) { (query, samplesOrNil, deletedObjectsOrNil, newAnchor, errorOrNil) in
            guard let samples = samplesOrNil, let deletedObjects = deletedObjectsOrNil else {
                print("*** An error occurred during the initial query: \(errorOrNil!.localizedDescription) ***")
                return
            }
            
            // TODO: find batches for re-upload?
            for sampleItem in samples {
                BatchStartDates.insert(self.beginning(of: .hour, date: sampleItem.startDate)!)
            }
            // TODO: find batches for re-upload?
            for deletedSampleItem in deletedObjects {
                print("deleted: \(deletedSampleItem)")
            }
            // TODO: Save the new anchor to the persisted store
            let data : Data = NSKeyedArchiver.archivedData(withRootObject: newAnchor as Any)
            UserDefaults.standard.set(data, forKey: anchorKey)
            print("BatchStartDates: \(BatchStartDates)")
        }
        healthKitStore.execute(query)
        
        return BatchStartDates
    }
    
    private func beginning(of component: Calendar.Component, date: Date?) -> Date? {
        let calendar = Calendar.current
        if component == .day {
            return calendar.startOfDay(for: date!)
        }

        var components: Set<Calendar.Component> {
            switch component {
            case .second:
                return [.year, .month, .day, .hour, .minute, .second]

            case .minute:
                return [.year, .month, .day, .hour, .minute]

            case .hour:
                return [.year, .month, .day, .hour]

            case .weekOfYear, .weekOfMonth:
                return [.yearForWeekOfYear, .weekOfYear]

            case .month:
                return [.year, .month]

            case .year:
                return [.year]

            default:
                return []
            }
        }

        guard !components.isEmpty else { return nil }
        return calendar.date(from: calendar.dateComponents(components, from: date!))
    }


    private func fetchIntradayStatisticsCollections(sampleType: HKQuantityType, batchDates: Set<Date>) {
        let calendar = Calendar.current
        var interval = DateComponents()
        interval.minute = 1
        
        // TODO: Calculate correctly anchor
        let anchorDate = calendar.date(byAdding: .hour, value: -2, to: Date())!
        
        // Exclude manually added data
        let wasUserEnteredPredicate = NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)
        
        let fromDate = Calendar.current.date(byAdding: .minute, value: -120, to: Date())
        let endDate = Calendar.current.date(byAdding: .minute, value: -60, to: Date())
        let startDatePredicate = HKQuery.predicateForSamples(withStart: fromDate, end: endDate, options: .strictStartDate)
        
        let fromDate2 = Calendar.current.date(byAdding: .minute, value: -5, to: Date())
        let startDatePredicate2 = HKQuery.predicateForSamples(withStart: fromDate2, end: Date(), options: .strictStartDate)
        
        
        let compound = NSCompoundPredicate(orPredicateWithSubpredicates: [startDatePredicate, startDatePredicate2]) //wasUserEnteredPredicate,

        let query = HKStatisticsCollectionQuery(quantityType: sampleType,
                                                quantitySamplePredicate: compound,
                                                options: [.cumulativeSum],
                                                anchorDate: anchorDate,
                                                intervalComponents: interval)
        // Set the results handler
        query.initialResultsHandler = {
            query, results, error in

            guard let statsCollection = results else {
                // Perform proper error handling here
                fatalError("*** An error occurred while calculating the statistics: \(error?.localizedDescription) ***")
            }

            let endDate = Date()

            statsCollection.enumerateStatistics(from: anchorDate, to: endDate) { [unowned self] statistics, stop in
                if let quantity = statistics.sumQuantity() {
                    let date = statistics.startDate
                    let value = quantity.doubleValue(for: HKUnit.kilocalorie())

                    print("date: \(date), value: \(value)")
                }
            }
        }

        healthKitStore.execute(query)
    }
    
    // TODO: Big question, are we need HKAnchoredObjectQuery or will be right use HKSampleQuery?
    private func fetchSampleUpdates(sampleType: HKSampleType) {
        // TODO: Calculate right anchor from persisted store
        var anchorDate = HKQueryAnchor.init(fromValue: 0)
        if let data = UserDefaults.standard.object(forKey: "\(sampleType)-Anchor-v1") as? Data {
            anchorDate = (NSKeyedUnarchiver.unarchiveObject(with: data) as? HKQueryAnchor)!
            print(anchorDate)
        }

        // Exclude manually added data
        let wasUserEnteredPredicate = NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)
        let fromDate = Calendar.current.date(byAdding: .day, value: -30, to: Date())
        let startDatePredicate = HKQuery.predicateForSamples(withStart: fromDate, end: Date(), options: .strictStartDate)
        let compound = NSCompoundPredicate(andPredicateWithSubpredicates: [startDatePredicate, wasUserEnteredPredicate])

        let query = HKAnchoredObjectQuery(type: sampleType,
                                              predicate: compound,
                                              anchor: anchorDate,
                                              limit: HKObjectQueryNoLimit) { (query, samplesOrNil, deletedObjectsOrNil, newAnchor, errorOrNil) in
            guard let samples = samplesOrNil, let deletedObjects = deletedObjectsOrNil else {
                print("*** An error occurred during the initial query: \(errorOrNil!.localizedDescription) ***")
                return
            }
            
            // TODO: find batches for re-upload?
            for sampleItem in samples {
                print("Sample item: \(sampleItem)")
            }

            // TODO: find batches for re-upload?
            for deletedSampleItem in deletedObjects {
                print("deleted: \(deletedSampleItem)")
            }
            
            let data : Data = NSKeyedArchiver.archivedData(withRootObject: newAnchor as Any)
            UserDefaults.standard.set(data, forKey: "\(sampleType)-Anchor-v1")
            
            // TODO: Save the new anchor to the persisted store
            // anchor = newAnchor!
        }
        healthKitStore.execute(query)
    }
    
//    func observe() {
//        self.authorizeHealthKitAccess { (success, error) in
//          guard success else { return }
//
//          let calendar = Calendar.current
//          // TODO: Need discuss with team, probably 30 days back will be limit
//          let earliestPermittedSampleDate = self.healthKitStore.earliestPermittedSampleDate()
//          let startDate = earliestPermittedSampleDate > calendar.startOfDay(for: Date()) ? earliestPermittedSampleDate : calendar.startOfDay(for: Date())
//
//          let sampleDataPredicate = HKQuery.predicateForSamples(withStart: startDate,
//                                                                      end: Date.distantFuture,
//                                                                      options: [])
//
//            // Enumerate each type, and setup an anchoredObjectQuery, which enables delivery of sample updates for each type.
//            for type in self.readableTypes {
//                // TODO: First Or create anchor with persisted store
//                var anchor: HKQueryAnchor? = nil
//
//                let query = HKAnchoredObjectQuery(type: type, predicate: sampleDataPredicate, anchor: anchor, limit: HKObjectQueryNoLimit) { (query, samplesOrNil, _, newAnchor, _) in
//                    self.updateHealthRecord(type: type, samplesOrNil: samplesOrNil)
//                    anchor = newAnchor
//                }
//
//                query.updateHandler = { (query, samplesOrNil, _, newAnchor, _) in
//                    self.updateHealthRecord(type: type, samplesOrNil: samplesOrNil)
//                    anchor = newAnchor
//                }
//
//                // Run the query.
//                self.healthKitStore.execute(query)
//            }
//        }
//    }
//
//    fileprivate func enableBackgroundDelivery() {
//        for type in readableTypes {
//            // TODO: Update frequency after finish testing
//            healthKitStore.enableBackgroundDelivery(for: type, frequency: .immediate, withCompletion: { (success, _) in
//                debugPrint("enabled background delivery \(success) for \(type)")
//            })
//        }
//    }
    
//    func registerWorkoutObserver() {
//        var query: HKObserverQuery = HKObserverQuery(sampleType: HKQuantityType.workoutType(), predicate: nil, updateHandler: self.observerCompletionHandler)
//
//        healthKitStore.execute(query)
//
//        // TODO: Update frequency after finish testing
//        healthKitStore.enableBackgroundDelivery(for: HKQuantityType.workoutType(), frequency: .immediate, withCompletion: {(succeeded: Bool, error: Error?) in
//           if succeeded{
//               print("Enabled background delivery for type \("workoutType")")
//           } else {
//               if let theError = error{
//                   print("Failed to enable background delivery of weight changes. ")
//                   print("Error = \(theError)")
//               }
//           }
//       })
//    }
    
//    func workoutChangedHandler(query: HKObserverQuery!, completionHandler: HKObserverQueryCompletionHandler!, error: Error?) {
//        print("Changed workouts in Health App")
//        print(query)
//
//        // TODO: Register tasks in queue
//
//        completionHandler()
//    }
}
