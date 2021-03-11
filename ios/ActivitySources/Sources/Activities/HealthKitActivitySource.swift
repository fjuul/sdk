import Foundation
import FjuulCore
import Alamofire

//sourcery: AutoMockable
protocol MountableHealthKitActivitySource: MountableActivitySource {
    func requestAccess(config: ActivitySourceConfigBuilder, completion: @escaping (Result<Void, Error>) -> Void)
}

/// The ActivitySource singleton class for the Healthkit tracker. This is an mountable activity source.
public final class HealthKitActivitySource: MountableHealthKitActivitySource {
    static public let shared = HealthKitActivitySource()

    var apiClient: ActivitySourcesApiClient?

    public var trackerValue = TrackerValue.HEALTHKIT

    private var healthKitManager: HealthKitManaging?

    init() {}

    /// Request show a modal prompting with list of required Healthkit data permissions based on provided ActivitySourceConfig.
    /// - Parameters:
    ///   - config: instance ActivitySourceConfig
    ///   - completion: void or error
    func requestAccess(config: ActivitySourceConfigBuilder, completion: @escaping (Result<Void, Error>) -> Void) {
        HealthKitManager.requestAccess(config: config) { result in
            completion(result)
        }
    }

    /// Sync HealthKit intraday data based on types and dates.
    /// - Parameters:
    ///   - startDate: Start date
    ///   - endDate: End date
    ///   - configTypes: list of HealthKitConfigType
    ///   - completion: void or error
    public func syncIntradayMetrics(startDate: Date, endDate: Date, configTypes: [HealthKitConfigType] = HealthKitConfigType.intradayTypes,
                                    completion: @escaping (Result<Void, Error>) -> Void) {
        guard let healthKitManager = self.healthKitManager else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .activitySourceNotMounted)))
            return
        }

        guard configTypes.allSatisfy(HealthKitConfigType.intradayTypes.contains) else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .illegalHealthKitConfigType)))
            return
        }

        healthKitManager.sync(startDate: startDate, endDate: endDate, configTypes: configTypes) { result in
            completion(result)
        }
    }

    /// Sync HealthKit workouts data based on specific dates.
    /// - Parameters:
    ///   - startDate: Start date
    ///   - endDate: End date
    ///   - completion: void or error
    public func syncWorkouts(startDate: Date, endDate: Date, completion: @escaping (Result<Void, Error>) -> Void) {
        guard let healthKitManager = self.healthKitManager else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .activitySourceNotMounted)))
            return
        }

        healthKitManager.sync(startDate: startDate, endDate: endDate, configTypes: [.workout]) { result in
            completion(result)
        }
    }

    /// Sync latest known user metrics (weight or height)
    /// - Parameters:
    ///   - configTypes: List of HealthKit types
    ///   - completion: void or error
    public func syncProfile(configTypes: [HealthKitConfigType] = HealthKitConfigType.userProfileTypes,
                            completion: @escaping (Result<Void, Error>) -> Void) {
        guard let healthKitManager = self.healthKitManager else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .activitySourceNotMounted)))
            return
        }

        guard configTypes.allSatisfy(HealthKitConfigType.userProfileTypes.contains) else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .illegalHealthKitConfigType)))
            return
        }

        healthKitManager.sync(startDate: nil, endDate: nil, configTypes: configTypes) { result in
            completion(result)
        }
    }

    /// Setup Healthkit backgroundDelivery for fetch desired data types based on ActivitySourceConfig
    /// - Parameters:
    ///   - apiClient: instance of ActivitySourcesApiClient
    ///   - config: instance of ActivitySourceConfigBuilder
    ///   - healthKitManagerBuilder: instance of HealthKitManagerBuilding
    ///   - completion: void or error
    internal func mount(apiClient: ActivitySourcesApiClient, config: ActivitySourceConfigBuilder,
                        healthKitManagerBuilder: HealthKitManagerBuilding,
                        completion: @escaping (Result<Void, Error>) -> Void) {

        self.apiClient = apiClient

        let healthKitManager = healthKitManagerBuilder.create(dataHandler: self.dataHandler)
        self.healthKitManager = healthKitManager

        healthKitManager.mount { result in
            completion(result)
        }
    }

    /// Disable Healthkit backgroundDelivery.
    /// - Parameter completion: void or error
    internal func unmount(completion: @escaping (Result<Void, Error>) -> Void) {
        guard let healthKitManager = self.healthKitManager else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .activitySourceNotMounted)))
            return
        }

        healthKitManager.disableAllBackgroundDelivery { result in
            completion(result)
        }
    }

    /// Handler for new data from backgroundDelivery or manual sync
    /// - Parameters:
    ///   - requestData: instance of HKBatchData
    ///   - completion: void or error
    private func dataHandler(_ requestData: HKRequestData?, completion: @escaping (Result<Void, Error>) -> Void) {
        guard let apiClient = self.apiClient else {
            completion(.failure(FjuulError.invalidConfig))
            return
        }
        guard let requestData = requestData else {
            completion(.success(()))
            return
        }

        switch requestData {
        case .batchData(let batchData):
            apiClient.sendHealthKitBatchData(data: batchData) { result in
                completion(result)
            }
        case .userProfileData(let userProfile):
            apiClient.sendHealthKitUserProfileData(data: userProfile) { result in
                completion(result)
            }
        }
    }
}
