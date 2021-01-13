import Foundation
import FjuulCore
import Alamofire

//sourcery: AutoMockable
protocol MountableActivitySourceHK: MountableActivitySource {
    func requestAccess(config: ActivitySourceConfigBuilder, completion: @escaping (Result<Bool, Error>) -> Void)
}

public final class ActivitySourceHK: MountableActivitySourceHK {
    static public let shared = ActivitySourceHK()

    var apiClient: ActivitySourcesApiClient?

    public var tracker = ActivitySourcesItem.healthkit
    public var persistor: Persistor?

    private var healthKitManager: HealthKitManager?

    private init() {}

    static func requestAccess(config: ActivitySourceConfigBuilder, completion: @escaping (Result<Bool, Error>) -> Void) {
        HealthKitManager.requestAccess(config: config) { result in
            completion(result)
        }
    }

    func requestAccess(config: ActivitySourceConfigBuilder, completion: @escaping (Result<Bool, Error>) -> Void) {
        HealthKitManager.requestAccess(config: config) { result in
            completion(result)
        }
    }

    func mount(apiClient: ActivitySourcesApiClient, config: ActivitySourceConfigBuilder, persistor: Persistor, healthKitManagerBuilder: HealthKitManagerBuilder, completion: @escaping (Result<Bool, Error>) -> Void) {
        self.apiClient = apiClient
        self.persistor = persistor

        let healthKitManager = healthKitManagerBuilder.create(dataHandler: self.dataHandler)
        self.healthKitManager = healthKitManager

        healthKitManager.mount { result in
            completion(result)
        }
    }

    func unmount(completion: @escaping (Result<Bool, Error>) -> Void) {
        guard let healthKitManager = self.healthKitManager else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .activitySourceNotMounted)))
            return
        }

        healthKitManager.disableAllBackgroundDelivery { result in
            completion(result)
        }
    }

    public func sync(completion: @escaping (Result<Bool, Error>) -> Void) {
        guard let healthKitManager = self.healthKitManager else {
            completion(.failure(FjuulError.activitySourceFailure(reason: .activitySourceNotMounted)))
            return
        }

        healthKitManager.sync { result in
            completion(result)
        }
    }

    private func dataHandler(_ requestData: HKRequestData?, completion: @escaping (Result<Bool, Error>) -> Void) {
        guard let requestData = requestData else {
            completion(.success(true))
            return
        }

        self.sendBatch(data: requestData) { result in
            completion(result)
        }
    }

    private func sendBatch(data: HKRequestData, completion: @escaping (Result<Bool, Error>) -> Void) {
        guard let apiClient = self.apiClient else { return  completion(.failure(FjuulError.invalidConfig))}

        let url = "\(apiClient.apiClient.baseUrl)/sdk/activity-sources/v1/\(apiClient.apiClient.userToken)/healthkit"

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .formatted(DateFormatters.iso8601Full)
        let parameterEncoder = JSONParameterEncoder(encoder: encoder)

        apiClient.apiClient.signedSession.request(url, method: .post, parameters: data, encoder: parameterEncoder).response { response in
            switch response.result {
            case .success:
                return completion(.success(true))
            case .failure(let err):
                return completion(.failure(err))
            }
        }
    }
}
