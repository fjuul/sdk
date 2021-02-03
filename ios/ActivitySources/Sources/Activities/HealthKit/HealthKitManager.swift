import Foundation
import HealthKit
import FjuulCore

protocol HealthKitManaging: AutoMockable {
    static var healthStore: HKHealthStore { get }

    init(anchorStore: HKAnchorStore, config: ActivitySourceConfigBuilder,
         dataHandler: @escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Void, Error>) -> Void) -> Void))
    static func requestAccess(config: ActivitySourceConfigBuilder, completion: @escaping (Result<Void, Error>) -> Void)
    func mount(completion: @escaping (Result<Void, Error>) -> Void)
    func disableAllBackgroundDelivery(completion: @escaping (Result<Void, Error>) -> Void)
    func sync(completion: @escaping (Result<Void, Error>) -> Void)
}

/// Manager for work with Healthkit permissions and data.
/// Setups background delivery and fetch data from HK when new event triger sync.
class HealthKitManager: HealthKitManaging {
    /// Shared HKHealthStore
    static let healthStore: HKHealthStore = HKHealthStore()

    private var anchorStore: HKAnchorStore
    private var dataHandler: ((_ data: HKRequestData?, _ completion: @escaping (Result<Void, Error>) -> Void) -> Void)
    private let serialQueue = DispatchQueue(label: "com.fjuul.sdk.queues.backgroundDelivery", qos: .userInitiated)
    private let config: ActivitySourceConfigBuilder

    required init(anchorStore: HKAnchorStore, config: ActivitySourceConfigBuilder,
                  dataHandler: @escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Void, Error>) -> Void) -> Void)) {
        self.config = config
        self.anchorStore = anchorStore
        self.dataHandler = dataHandler
    }

    /// Requests access to all the data types the app wishes to read from HealthKit.
    static func requestAccess(config: ActivitySourceConfigBuilder, completion: @escaping (Result<Void, Error>) -> Void) {
        guard HKHealthStore.isHealthDataAvailable() else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitNotAvailableOnDevice)))
            return
        }

        HealthKitManager.healthStore.requestAuthorization(toShare: nil, read: config.healthKitConfig.typesToRead) { (success: Bool, error: Error?) in
            if let err = error {
                completion(.failure(err))
            } else if !success {
                completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitAuthorization)))
            } else {
                completion(.success(()))
            }
        }
    }

    /// On success observer queries are set up for background delivery.
    /// This is safe to call repeatedly and should be called at least once per launch.
    /// - Parameter completion: void or error
    func mount(completion: @escaping (Result<Void, Error>) -> Void) {
        guard HKHealthStore.isHealthDataAvailable() else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitNotAvailableOnDevice)))
            return
        }

        HealthKitManager.requestAccess(config: self.config) { result in
            switch result {
            case .success:
                self.setUpBackgroundDeliveryForDataTypes()
                completion(.success(()))
            case .failure(let err):
                completion(.failure(err))
            }
        }
    }

    /// Disables all BackgroundDelivery observers
    /// - Parameter completion: void or error
    func disableAllBackgroundDelivery(completion: @escaping (Result<Void, Error>) -> Void) {
        HealthKitManager.healthStore.disableAllBackgroundDelivery { (success: Bool, error: Error?) in
            if let error = error {
                completion(.failure(error))
            } else if !success {
                completion(.failure(FjuulError.activitySourceFailure(reason: .backgroundDeliveryNotDisabled)))
            } else {
                completion(.success(()))
            }
        }
    }

    /// Force start sync
    /// - Parameter completion: void or error
    func sync(completion: @escaping (Result<Void, Error>) -> Void) {
        let group = DispatchGroup()
        var error: Error?

        for sampleType in self.config.healthKitConfig.typesToRead {
            group.enter()

            self.queryForUpdates(sampleType: sampleType) { data, newAnchor in
                self.dataHandler(data) { result in
                    switch result {
                    case .success:
                        do {
                            try self.anchorStore.save(type: sampleType, newAnchor: newAnchor)
                        } catch {
                            DataLogger.shared.error("Unexpected error: \(error).")
                        }
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
                completion(.success(()))
            }
        }
    }

    /// Sets up the observer queries for background health data delivery. Based on Healthkit config.
    /// Observer callbacks handleds syncroniously via serial queue, for void issue with inconsisted saved anchors in persisted store.
    ///
    /// - parameter types: Set of `HKObjectType` to observe changes to.
    private func setUpBackgroundDeliveryForDataTypes() {
        for type in self.config.healthKitConfig.typesToRead {
            let sampleType = type as HKSampleType

            let query = HKObserverQuery(sampleType: sampleType, predicate: nil) { (_, completionHandler: @escaping HKObserverQueryCompletionHandler, error: Error?) in

                // Semaphore need for wait async task and correct notice queue about finish async task
                self.serialQueue.async {
                    let semaphore = DispatchSemaphore(value: 0)
                    self.queryForUpdates(sampleType: type) { data, newAnchor in
                        self.dataHandler(data) { result in
                            switch result {
                            case .success:
                                do {
                                    defer { DataLogger.shared.info("Success handled backgroundDelivery for \(sampleType)") }
                                    try self.anchorStore.save(type: sampleType, newAnchor: newAnchor)
                                } catch {
                                    DataLogger.shared.error("Unexpected error: \(error).")
                                }
                            case .failure(let err):
                                DataLogger.shared.error("Failure on handled backgroundDelivery for \(sampleType) with error: \(err)")
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
                if success {
                    DataLogger.shared.info("Register backgroundDelivery for type \(sampleType)")
                } else {
                    DataLogger.shared.error("Was not able register backgroundDelivery for type \(sampleType), with error \(String(describing: error))")
                }
            }
        }
    }

    /// Initiates HK queries for new data based on the given type
    ///
    /// - parameter sampleType: `HKObjectType` which has new data avilable.
    /// - parameter completion: HKRequestData? and the new anchore
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
        guard let anchor = try? self.anchorStore.get(type: HKObjectType.workoutType()) else {
            completion(nil, nil)
            return
        }

        let predicatBuilder = HealthKitQueryPredictateBuilder(healthKitConfig: self.config.healthKitConfig)

        WorkoutFetcher.fetch(anchor: anchor, predictateBuilder: predicatBuilder) { requestData, newAnchor in

            completion(requestData, newAnchor)
        }
    }

    private func fetchIntradayUpdates(sampleType: HKQuantityType, completion: @escaping (_ data: HKRequestData?, _ newAnchor: HKQueryAnchor?) -> Void) {
        guard let anchor = try? self.anchorStore.get(type: sampleType) else {
            completion(nil, nil)
            return
        }

        let predicatBuilder = HealthKitQueryPredictateBuilder(healthKitConfig: self.config.healthKitConfig)

        AggregatedDataFetcher.fetch(type: sampleType, anchor: anchor, predictateBuilder: predicatBuilder) { hkRequestData, newAnchor in

            completion(hkRequestData, newAnchor)
        }
    }
}
