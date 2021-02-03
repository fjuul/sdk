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

    /// Force initiate sync data
    /// - Parameter completion: void or error
    public func sync(completion: @escaping (Result<Void, Error>) -> Void) {
        guard let healthKitManager = self.healthKitManager else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .activitySourceNotMounted)))
            return
        }

        healthKitManager.sync { result in
            completion(result)
        }
    }

    /// Setup Healthkit backgroundDelivery for fetch disired data types based on ActivitySourceConfig
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
    ///   - requestData: instance of HKRequestData
    ///   - completion: void or error
    private func dataHandler(_ requestData: HKRequestData?, completion: @escaping (Result<Void, Error>) -> Void) {
        guard let requestData = requestData else {
            completion(.success(()))
            return
        }

        self.sendBatch(data: requestData) { result in
            completion(result)
        }
    }

    /// Sends data to back-end
    /// - Parameters:
    ///   - data: instance of HKRequestData
    ///   - completion: void or error
    private func sendBatch(data: HKRequestData, completion: @escaping (Result<Void, Error>) -> Void) {
        guard let apiClient = self.apiClient else { return  completion(.failure(FjuulError.invalidConfig))}

        let url = "\(apiClient.apiClient.baseUrl)/sdk/activity-sources/v1/\(apiClient.apiClient.userToken)/healthkit"

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .formatted(DateFormatters.iso8601Full)
        let parameterEncoder = JSONParameterEncoder(encoder: encoder)

        apiClient.apiClient.signedSession.request(url, method: .post, parameters: data, encoder: parameterEncoder).response { response in
            switch response.result {
            case .success:
                return completion(.success(()))
            case .failure(let err):
                return completion(.failure(err))
            }
        }
    }
}
