#if canImport(HealthKit)
import Foundation
import HealthKit
import FjuulCore
import Alamofire

//public typealias PartialHKRequestData = Partial<HKRequestData>

class HKDataManager {
    internal let healthKitStore: HKHealthStore = HKHealthStore()
    internal let apiClient: ApiClient
    internal var hkAnchorStore: HKAnchorStore

    init(apiClient: ApiClient, persistor: Persistor) {
        self.apiClient = apiClient
        self.hkAnchorStore = HKAnchorStore(persistor: persistor)
    }

    var readableTypes: Set<HKSampleType> {
        return samplesReadableTypes.union(intradayReadableTypes)
    }
    var intradayReadableTypes: Set<HKQuantityType> {
        // TODO: types should be based on config
        return [
            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!,
            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!,
//            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceCycling)!,
//            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceWalkingRunning)!
        ]
    }
    var samplesReadableTypes: Set<HKQuantityType> {
        // TODO: types should be based on config
        return [
//            HKWorkoutType.workoutType(),
            HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.heartRate)!,
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

                self.fetchSampleUpdates(sampleType: sampleType) { data in
                    print("HR Data", data)
                }
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
    private func fetchSampleUpdates(sampleType: HKQuantityType, completion: @escaping ([HrDataPoint]) -> Void) {
        let anchorDate = self.hkAnchorStore.anchor?.heartRate ?? HKQueryAnchor.init(fromValue: 0)

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
                // TODO: Refcatoring for bring right response (Result/Error)
                return
            }
            
            var hrDataPoints: [HrDataPoint] = []

            for sampleItem in samples {
                let hrItem = HrDataPoint(uuid: sampleItem.uuid,
                            value: sampleItem.quantity.doubleValue(for: HKUnit.countUnit().unitDividedByUnit(HKUnit.minuteUnit())),
                            startDate: sampleItem.startDate,
                            endDate: sampleItem.endDate,
//                            source: sampleItem.sourceRevision,
                            metadata: sampleItem.metadata)

                hrDataPoints.append(hrItem)
            }
            self.hkAnchorStore.anchor?.heartRate = newAnchor
            completion(hrDataPoints)
        }
        healthKitStore.execute(query)
    }

    private func fetchIntradayUpdates(sampleType: HKQuantityType) {
        self.getBatchSegments(sampleType: sampleType) { (batchStartDates) in
            print("batchStartDates: \(batchStartDates)")
            self.fetchIntradayStatisticsCollections(sampleType: sampleType, batchDates: batchStartDates) { results in
                let requestData = self.buildHKRequestData(data: results, sampleType: sampleType)
                self.sendHKData(data: requestData) { result in
                    switch result {
                    case .success:
                        print("SUCESS REQUEST")
                    case .failure(let err):
                        print(err)
                    }
                }
            }
        }
    }
    
    private func buildHKRequestData(data: [HKStatistics], sampleType: HKQuantityType) -> HKRequestData {
        let batches = HKBatchAggregator(data: data, sampleType: sampleType).generate()
        print("got results, \(sampleType) \(batches.count)")
        
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!:
           return HKRequestData(caloriesData: batches)
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!:
            return HKRequestData(stepsData: batches)
        default:
            return HKRequestData()
        }
    }
    
    private func getBatchSegments(sampleType: HKQuantityType, completion: @escaping (Set<Date>) -> Void) {
        var batchStartDates: Set<Date> = []

        let anchorDate = self.getAnchorBySampleType(sampleType: sampleType)

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

            self.saveAnchorBySampleType(newAnchor: newAnchor, sampleType: sampleType)
            completion(batchStartDates)
        }
        healthKitStore.execute(query)
    }

    private func getAnchorBySampleType(sampleType: HKQuantityType) -> HKQueryAnchor {
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!:
           return self.hkAnchorStore.anchor?.activeEnergyBurned ?? HKQueryAnchor.init(fromValue: 0)
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!:
            return self.hkAnchorStore.anchor?.stepCount ?? HKQueryAnchor.init(fromValue: 0)
        default:
            return HKQueryAnchor.init(fromValue: 0)
        }
    }

    private func saveAnchorBySampleType(newAnchor: HKQueryAnchor?, sampleType: HKQuantityType) -> Void {
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!:
            self.hkAnchorStore.anchor?.activeEnergyBurned = newAnchor
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!:
            self.hkAnchorStore.anchor?.stepCount = newAnchor
        default:
            return
        }
    }

    private func fetchIntradayStatisticsCollections(sampleType: HKQuantityType, batchDates: Set<Date>, completion: @escaping ([HKStatistics]) -> Void) {
        let calendar = Calendar.current
        var interval = DateComponents()
        interval.minute = 1

        // Exclude manually added data
        // let wasUserEnteredPredicate = NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)
        let datePredicates = self.statisticsCollectionsDatePredicates(batchDates: batchDates)

        let compound = NSCompoundPredicate(orPredicateWithSubpredicates: datePredicates)

        // Always start from beginning of hour
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

    // TODO: Continue work on that code after merge https://github.com/fjuul/sdk-server/pull/791
    // TODO: Extract method to separate class
    private func sendHKData(data: HKRequestData, completion: @escaping (Result<Data, Error>) -> Void) {
        let url = "\(apiClient.baseUrl)/\(apiClient.userToken)/healthkit"

        apiClient.signedSession.request(url, method: .post, parameters: data, encoder: URLEncodedFormParameterEncoder(destination: .methodDependent)).apiResponse { response in
            return completion(response.result)
        }
    }

}
#endif
