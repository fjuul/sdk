import Foundation
import HealthKit
import FjuulCore

/// Helper for reading and writing to HealthKit.
class HealthKitManager {
    static private let healthStore = HKHealthStore()

    private var persistor: Persistor
    private var hkAnchorStore: HKAnchorStore
    public var dataHandler: ((_ data: HKRequestData) -> Void)?
    private let serialQueue = DispatchQueue(label: "com.fjuul.sdk.queues.backgroundDelivery", qos: .userInitiated)

    init(persistor: Persistor) {
        self.persistor = persistor
        self.hkAnchorStore = HKAnchorStore(persistor: persistor)
    }

    /// Requests access to all the data types the app wishes to read/write from HealthKit.
    static func requestAccess(completion: @escaping (Result<Bool, Error>) -> Void) {
        guard HKHealthStore.isHealthDataAvailable() else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitNotAvailableOnDevice)))
            return
        }

        HealthKitManager.healthStore.requestAuthorization(toShare: nil, read: dataTypesToRead()) { (success: Bool, error: Error?) in
            if let err = error {
                completion(.failure(err))
            } else {
                completion(.success(success))
            }
        }
    }
    
    /// Types of data  Fjull wishes to read from HealthKit.
    /// - returns: A set of HKObjectType.
    private static func dataTypesToRead() -> Set<HKSampleType> {
        return Set(arrayLiteral: HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!,
                       HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!,
                       HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceCycling)!,
                       HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceWalkingRunning)!
//                       HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.heartRate)!,
//                       HKObjectType.workoutType()
        )
    }

    /// On success observer queries are set up for background delivery.
    /// This is safe to call repeatedly and should be called at least once per launch.
    func mount(completion: @escaping (Result<Bool, Error>) -> Void) {
        guard HKHealthStore.isHealthDataAvailable() else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitNotAvailableOnDevice)))
            return
        }

        self.setUpBackgroundDeliveryForDataTypes(types: HealthKitManager.dataTypesToRead())
    }

    func disableAllBackgroundDelivery(completion: @escaping (Result<Bool, Error>) -> Void) {
        HealthKitManager.healthStore.disableAllBackgroundDelivery { (success: Bool, error: Error?) in
            if success {
                completion(.success(success))
            } else {
                if let error = error {
                    completion(.failure(error))
                } else {
                    completion(.success(false))
                }
            }
        }
    }

    /// Sets up the observer queries for background health data delivery.
    ///
    /// - parameter types: Set of `HKObjectType` to observe changes to.
    private func setUpBackgroundDeliveryForDataTypes(types: Set<HKSampleType>) {
        for type in types {
            guard let sampleType = type as? HKSampleType else { print("ERROR: \(type) is not an HKSampleType"); continue }
            guard let handler = self.dataHandler else { return }

            let query = HKObserverQuery(sampleType: sampleType, predicate: nil) { (query: HKObserverQuery, completionHandler: HKObserverQueryCompletionHandler, error: Error?) in
//                print("observer query update handler called for type \(type), error: \(error)")

                // Semaphore need for wait async task and correct notice queue about finish task
                self.serialQueue.async {
                    let semaphore = DispatchSemaphore(value: 0)
                    self.queryForUpdates(type: type) { data in
                        handler(data)
                        semaphore.signal()
                    }
                    _ = semaphore.wait(wallTimeout: .distantFuture)
                }

                // TODO: Refactoring for call correctly completionHandler
                completionHandler()
            }

            HealthKitManager.healthStore.execute(query)
            HealthKitManager.healthStore.enableBackgroundDelivery(for: type, frequency: .immediate) { (success: Bool, error: Error?) in
//                print("enableBackgroundDeliveryForType handler called for \(type) - success: \(success), error: \(error)")
            }
        }
    }

    /// Initiates an `HKAnchoredObjectQuery` for each type of data that the app reads and stores
    /// the result as well as the new anchor.
    func readHealthKitData() {
        if let handler = self.dataHandler {
            //handler("on Setup")
        }
    }

    /// Initiates HK queries for new data based on the given type
    ///
    /// - parameter type: `HKObjectType` which has new data avilable.
    private func queryForUpdates(type: HKSampleType, completion: @escaping (_ data: HKRequestData) -> Void) {
        switch type {
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!:
            self.fetchIntradayUpdates(type: type) { (data) in
                completion(data)
            }
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!:
            self.fetchIntradayUpdates(type: type) { (data) in
                completion(data)
            }
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceCycling)!:
            self.fetchIntradayUpdates(type: type) { (data) in
                completion(data)
            }
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceWalkingRunning)!:
            self.fetchIntradayUpdates(type: type) { (data) in
                completion(data)
            }
        default: print("Unhandled HKObjectType: \(type)")
        }
    }

    private func fetchIntradayUpdates(type: HKSampleType, completion: @escaping (_ data: HKRequestData) -> Void) {
        self.getBatchSegments(sampleType: type) { batchStartDates in
            self.fetchIntradayStatisticsCollections(sampleType: type, batchDates: batchStartDates) { results in

                let hkRequestData = self.buildRequestData(data: results, sampleType: (type as? HKQuantityType)!)

                completion(hkRequestData)
            }
        }
    }

    private func fetchIntradayStatisticsCollections(sampleType: HKSampleType, batchDates: Set<Date>, completion: @escaping ([HKStatistics]) -> Void) {
        let calendar = Calendar.current
        var interval = DateComponents()
        interval.minute = 1

        // Exclude manually added data
        // let wasUserEnteredPredicate = NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)
        let datePredicates = self.statisticsCollectionsDatePredicates(batchDates: batchDates)

        let compound = NSCompoundPredicate(orPredicateWithSubpredicates: datePredicates)

        // Always start from beginning of hour
        let anchorDate = HKDataUtils.beginningOfHour(date: calendar.date(byAdding: .day, value: -30, to: Date()))!

        let query = HKStatisticsCollectionQuery(quantityType: (sampleType as? HKQuantityType)!,
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

        HealthKitManager.healthStore.execute(query)
    }

    private func getBatchSegments(sampleType: HKSampleType, completion: @escaping (Set<Date>) -> Void) {
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
        HealthKitManager.healthStore.execute(query)
    }

    private func getAnchorBySampleType(sampleType: HKObjectType) -> HKQueryAnchor {
//        return HKQueryAnchor.init(fromValue: 0)
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!:
            return self.hkAnchorStore.anchors?[.activeEnergyBurned] ?? HKQueryAnchor.init(fromValue: 0)
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!:
            return self.hkAnchorStore.anchors?[.stepCount] ?? HKQueryAnchor.init(fromValue: 0)
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceCycling)!:
            return self.hkAnchorStore.anchors?[.distanceCycling] ?? HKQueryAnchor.init(fromValue: 0)
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceWalkingRunning)!:
            return self.hkAnchorStore.anchors?[.distanceWalkingRunning] ?? HKQueryAnchor.init(fromValue: 0)
        default:
            return HKQueryAnchor.init(fromValue: 0)
        }
    }

    private func saveAnchorBySampleType(newAnchor: HKQueryAnchor?, sampleType: HKObjectType) {
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!:
            self.hkAnchorStore.anchors?[.activeEnergyBurned] = newAnchor
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!:
            self.hkAnchorStore.anchors?[.stepCount] = newAnchor
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceCycling)!:
            self.hkAnchorStore.anchors?[.distanceCycling] = newAnchor
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceWalkingRunning)!:
            self.hkAnchorStore.anchors?[.distanceWalkingRunning] = newAnchor
        default:
            return
        }
    }

    private func statisticsCollectionsDatePredicates(batchDates: Set<Date>) -> [NSPredicate] {
        var predicates: [NSPredicate] = []

        batchDates.forEach { (date) in
            predicates.append(HKQuery.predicateForSamples(withStart: date, end: HKDataUtils.endOfHour(date: date)!, options: .strictStartDate))
        }

        return predicates
    }

    private func buildRequestData(data: [HKStatistics], sampleType: HKQuantityType) -> HKRequestData {
        let batches = HKBatchAggregator(data: data, sampleType: sampleType).generate()

        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.activeEnergyBurned)!:
           return HKRequestData(caloriesData: batches)
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.stepCount)!:
            return HKRequestData(stepsData: batches)
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceCycling)!:
            return HKRequestData(cyclingData: batches)
        case HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier.distanceWalkingRunning)!:
            return HKRequestData(walkingData: batches)
        default:
            return HKRequestData()
        }
    }
}
