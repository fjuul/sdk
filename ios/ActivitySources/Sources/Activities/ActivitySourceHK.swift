import Foundation
import FjuulCore
import Alamofire

final class ActivitySourceHK: ActivitySource {
    static public let shared = ActivitySourceHK()

    var tracker = ActivitySourcesItem.healthkit
    var apiClient: ApiClient?
    var persistor: Persistor?

    private var healthKitManager: HealthKitManager?

    private init() {}

    static func requestAccess(completion: @escaping (Result<Bool, Error>) -> Void) {
        HealthKitManager.requestAccess { result in
            completion(result)
        }
    }

    func mount(apiClient: ApiClient, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void) {
        self.apiClient = apiClient
        self.persistor = persistor

        let healthKitManager = HealthKitManager(persistor: persistor, dataHandler: self.hkDataHandler)
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

    private func hkDataHandler(_ requestData: HKRequestData?, completion: @escaping (Result<Bool, Error>) -> Void) {
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

        let url = "\(apiClient.baseUrl)/sdk/activity-sources/v1/\(apiClient.userToken)/healthkit"

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .formatted(DateFormatters.iso8601Full)
        let parameterEncoder = JSONParameterEncoder(encoder: encoder)

        apiClient.signedSession.request(url, method: .post, parameters: data, encoder: parameterEncoder).response { response in
            switch response.result {
            case .success:
                return completion(.success(true))
            case .failure(let err):
                return completion(.failure(err))
            }
        }
    }
}
