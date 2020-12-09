import Foundation
import HealthKit
import FjuulCore

class HealthKitManager {
    static var healthStore: HKHealthStore = HKHealthStore()

    private var persistor: Persistor
    private var hkAnchorStore: HKAnchorStore
    private var dataHandler: ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)
    private let serialQueue = DispatchQueue(label: "com.fjuul.sdk.queues.backgroundDelivery", qos: .userInitiated)
    private let config: ActivitySourceConfigBuilder

    init(persistor: Persistor, config: ActivitySourceConfigBuilder, dataHandler: @escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)) {
        self.config = config
        self.persistor = persistor
        self.hkAnchorStore = HKAnchorStore(persistor: persistor)
        self.dataHandler = dataHandler
    }

    /// Requests access to all the data types the app wishes to read/write from HealthKit.
    static func requestAccess(config: ActivitySourceConfigBuilder, completion: @escaping (Result<Bool, Error>) -> Void) {
        guard HKHealthStore.isHealthDataAvailable() else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitNotAvailableOnDevice)))
            return
        }

        HealthKitManager.healthStore.requestAuthorization(toShare: nil, read: dataTypesToRead(config: config)) { (success: Bool, error: Error?) in
            if let err = error {
                completion(.failure(err))
            } else {
                completion(.success(success))
            }
        }
    }

    /// Types of data  Fjull wishes to read from HealthKit.
    /// - returns: A set of HKObjectType.
    private static func dataTypesToRead(config: ActivitySourceConfigBuilder) -> Set<HKSampleType> {
        var dataTypes: Set<HKSampleType> = []

        if config.healthKitConfig.dataTypesToRead.contains(.activeEnergyBurned) {
            dataTypes.insert(HKObjectType.quantityType(forIdentifier: .activeEnergyBurned)!)
        }

        if config.healthKitConfig.dataTypesToRead.contains(.distanceCycling) {
            dataTypes.insert(HKObjectType.quantityType(forIdentifier: .distanceCycling)!)
        }

        if config.healthKitConfig.dataTypesToRead.contains(.stepCount) {
            dataTypes.insert(HKObjectType.quantityType(forIdentifier: .stepCount)!)
        }

        if config.healthKitConfig.dataTypesToRead.contains(.distanceWalkingRunning) {
            dataTypes.insert(HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning)!)
        }

        if config.healthKitConfig.dataTypesToRead.contains(.heartRate) {
            dataTypes.insert(HKObjectType.quantityType(forIdentifier: .heartRate)!)
        }

        if config.healthKitConfig.dataTypesToRead.contains(.workoutType) {
            dataTypes.insert(HKObjectType.workoutType())
        }
//
        return dataTypes
    }

    /// On success observer queries are set up for background delivery.
    /// This is safe to call repeatedly and should be called at least once per launch.
    func mount(completion: @escaping (Result<Bool, Error>) -> Void) {
        guard HKHealthStore.isHealthDataAvailable() else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitNotAvailableOnDevice)))
            return
        }

        HealthKitManager.requestAccess(config: self.config) { result in
            switch result {
            case .success:
                self.setUpBackgroundDeliveryForDataTypes()
                completion(.success(true))
            case .failure(let err):
                completion(.failure(err))
            }
        }
    }

    func disableAllBackgroundDelivery(completion: @escaping (Result<Bool, Error>) -> Void) {
        HealthKitManager.healthStore.disableAllBackgroundDelivery { (success: Bool, error: Error?) in
            if let error = error {
                completion(.failure(error))
                return
            }

            completion(.success(success))
        }
    }

    func sync(completion: @escaping (Result<Bool, Error>) -> Void) {
        let group = DispatchGroup()
        var error: Error?

        for sampleType in HealthKitManager.dataTypesToRead(config: self.config) {
            group.enter()

            self.queryForUpdates(sampleType: sampleType) { data, newAnchor in
                self.dataHandler(data) { result in
                    switch result {
                    case .success:
                        print("SUCESS sync for \(sampleType), with newAnchor: \(newAnchor)")
                        self.saveAnchorBySampleType(newAnchor: newAnchor, sampleType: sampleType)
                    case .failure(let err):
                        error = err
                    }

                    group.leave()
                }
            }
        }

        group.notify(queue: DispatchQueue.global()) {
            if let err = error {
                completion(.failure(err))
            } else {
                completion(.success(true))
            }
        }
    }

    /// Sets up the observer queries for background health data delivery.
    ///
    /// - parameter types: Set of `HKObjectType` to observe changes to.
    private func setUpBackgroundDeliveryForDataTypes() {
        let types = HealthKitManager.dataTypesToRead(config: self.config)

        for type in types {
            guard let sampleType = type as? HKSampleType else { continue }

            let query = HKObserverQuery(sampleType: sampleType, predicate: nil) { (query: HKObserverQuery, completionHandler: @escaping HKObserverQueryCompletionHandler, error: Error?) in

                // Semaphore need for wait async task and correct notice queue about finish async task
                self.serialQueue.async {
                    let semaphore = DispatchSemaphore(value: 0)
                    self.queryForUpdates(sampleType: type) { data, newAnchor in
                        self.dataHandler(data) { result in
                            switch result {
                            case .success:
                                print("SUCESS hadled backgroundDelivery for \(sampleType), with newAnchor: \(newAnchor)")
                                self.saveAnchorBySampleType(newAnchor: newAnchor, sampleType: sampleType)
                            case .failure(let err):
                                // TODO: Add logging here
                                print("HTTP error:", err)
                            }

                            semaphore.signal()
                            // You must call this block as soon as you are done processing the incoming data. Calling this block tells HealthKit that you have
                            // successfully received the background data. If you do not call this block, HealthKit continues to attempt to launch your app using
                            // a back off algorithm. If your app fails to respond three times, HealthKit assumes that your app cannot receive data, and stops
                            // sending you background updates.
                            completionHandler()
                        }
                    }
                    _ = semaphore.wait(timeout: .now() + 120)
                }
            }

            HealthKitManager.healthStore.execute(query)
            HealthKitManager.healthStore.enableBackgroundDelivery(for: type, frequency: .hourly) { (success: Bool, error: Error?) in
              //TODO: Add logging here: print("enableBackgroundDeliveryForType handler called for \(type) - success: \(success), error: \(error)")
            }
        }
    }

    /// Initiates HK queries for new data based on the given type
    ///
    /// - parameter type: `HKObjectType` which has new data avilable.
    private func queryForUpdates(sampleType: HKSampleType, completion: @escaping (_ data: HKRequestData?, _ newAnchor: HKQueryAnchor?) -> Void) {
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: .activeEnergyBurned),
             HKObjectType.quantityType(forIdentifier: .distanceCycling),
             HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning),
             HKObjectType.quantityType(forIdentifier: .heartRate),
             HKObjectType.quantityType(forIdentifier: .stepCount):
            self.fetchIntradayUpdates(sampleType: (sampleType as? HKQuantityType)!) { (data, newAnchor) in
                completion(data, newAnchor)
            }
        case HKObjectType.workoutType():
            self.fetchWorkoutsUpdates { (data, newAnchor) in
                completion(data, newAnchor)
            }
        default:
            completion(nil, nil)
        }
    }

    private func fetchWorkoutsUpdates(completion: @escaping (_ data: HKRequestData?, _ newAnchor: HKQueryAnchor?) -> Void) {
        let anchor = self.getAnchorBySampleType(sampleType: HKObjectType.workoutType())
        let predicate = self.samplesPredicate()

        WorkoutFetcher.fetch(anchor: anchor, predicate: predicate) { workouts, newAnchor in
            let requestData = workouts.count > 0 ? HKRequestData(workouts: workouts) : nil
            completion(requestData, newAnchor)
        }
    }

    private func fetchIntradayUpdates(sampleType: HKQuantityType, completion: @escaping (_ data: HKRequestData?, _ newAnchor: HKQueryAnchor?) -> Void) {
        self.getIntradayBatchSegments(sampleType: sampleType) { batchDates, newAnchor in
            if sampleType == HKObjectType.quantityType(forIdentifier: .heartRate) {
                AggregatedDataFetcher.fetchHrData(predicate: self.intradatPredicates(batchDates: batchDates), batchDates: batchDates) { results in
                    let hkRequestData = HKRequestData.build(sampleType: sampleType, batches: results)

                    completion(results.count > 0 ? hkRequestData : nil, newAnchor)
                }
            } else {
                AggregatedDataFetcher.fetchIntradayData(sampleType: sampleType, predicate: self.intradatPredicates(batchDates: batchDates), batchDates: batchDates) { results in
                    let hkRequestData = HKRequestData.build(sampleType: sampleType, batches: results)

                    completion(results.count > 0 ? hkRequestData : nil, newAnchor)
                }
            }
        }
    }

    private func intradatPredicates(batchDates: Set<Date>) -> NSCompoundPredicate {
        let datePredicates = NSCompoundPredicate(type: .or, subpredicates: self.statisticsCollectionsDatePredicates(batchDates: batchDates))

        if self.config.healthKitConfig.syncUserEnteredData {
            // Exclude manually added data
            let wasUserEnteredPredicate = NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)

            return NSCompoundPredicate(type: .and, subpredicates: [datePredicates, wasUserEnteredPredicate])
        } else {
            return datePredicates
        }
    }

    private func samplesPredicate() -> NSCompoundPredicate {
        let fromDate = Calendar.current.date(byAdding: .day, value: -30, to: Date())
        let startDatePredicate = HKQuery.predicateForSamples(withStart: fromDate, end: Date(), options: .strictStartDate)

        if self.config.healthKitConfig.syncUserEnteredData {
            let wasUserEnteredPredicate = NSPredicate(format: "metadata.%K != YES", HKMetadataKeyWasUserEntered)

            return NSCompoundPredicate(andPredicateWithSubpredicates: [startDatePredicate, wasUserEnteredPredicate])
        } else {
            return NSCompoundPredicate(andPredicateWithSubpredicates: [startDatePredicate])
        }
    }

    private func getIntradayBatchSegments(sampleType: HKSampleType, completion: @escaping (Set<Date>, HKQueryAnchor?) -> Void) {
        var batchStartDates: Set<Date> = []

        let anchorDate = self.getAnchorBySampleType(sampleType: sampleType)

        let query = HKAnchoredObjectQuery(type: sampleType,
                                          predicate: self.samplesPredicate(),
                                          anchor: anchorDate,
                                          limit: HKObjectQueryNoLimit) { (_, samples, _, newAnchor, error) in
            guard let samples = samples else {
                completion(batchStartDates, newAnchor)
                return
            }

            for sampleItem in samples {
                batchStartDates.insert(HKDataUtils.beginningOfHour(date: sampleItem.startDate)!)
            }

            completion(batchStartDates, newAnchor)
        }
        HealthKitManager.healthStore.execute(query)
    }

    private func getAnchorBySampleType(sampleType: HKObjectType) -> HKQueryAnchor {
        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: .activeEnergyBurned):
            return self.hkAnchorStore.anchors?[.activeEnergyBurned] ?? HKQueryAnchor.init(fromValue: 0)
        case HKObjectType.quantityType(forIdentifier: .stepCount):
            return self.hkAnchorStore.anchors?[.stepCount] ?? HKQueryAnchor.init(fromValue: 0)
        case HKObjectType.quantityType(forIdentifier: .distanceCycling):
            return self.hkAnchorStore.anchors?[.distanceCycling] ?? HKQueryAnchor.init(fromValue: 0)
        case HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning):
            return self.hkAnchorStore.anchors?[.distanceWalkingRunning] ?? HKQueryAnchor.init(fromValue: 0)
        case HKObjectType.quantityType(forIdentifier: .heartRate):
            return self.hkAnchorStore.anchors?[.heartRate] ?? HKQueryAnchor.init(fromValue: 0)
        case HKObjectType.workoutType():
            return self.hkAnchorStore.anchors?[.workout] ?? HKQueryAnchor.init(fromValue: 0)
        default:
            return HKQueryAnchor.init(fromValue: 0)
        }
    }

    private func saveAnchorBySampleType(newAnchor: HKQueryAnchor?, sampleType: HKObjectType) {
        guard let newAnchor = newAnchor else {
            return
        }

        switch sampleType {
        case HKObjectType.quantityType(forIdentifier: .activeEnergyBurned):
            self.hkAnchorStore.anchors?[.activeEnergyBurned] = newAnchor
        case HKObjectType.quantityType(forIdentifier: .stepCount):
            self.hkAnchorStore.anchors?[.stepCount] = newAnchor
        case HKObjectType.quantityType(forIdentifier: .distanceCycling):
            self.hkAnchorStore.anchors?[.distanceCycling] = newAnchor
        case HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning):
            self.hkAnchorStore.anchors?[.distanceWalkingRunning] = newAnchor
        case HKObjectType.quantityType(forIdentifier: .heartRate):
            self.hkAnchorStore.anchors?[.heartRate] = newAnchor
        case HKObjectType.workoutType():
            self.hkAnchorStore.anchors?[.workout] = newAnchor
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
}
