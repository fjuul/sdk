import Foundation
import FjuulCore
import Alamofire

final class ActivitySourceHK: ActivitySourceProtocol {
    static public let shared = ActivitySourceHK()

    var apiClient: ApiClient?
    var persistor: Persistor?

    private var healthKitManager: HealthKitManager?

    private init() {}

    func mount(apiClient: ApiClient, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void) {
        self.apiClient = apiClient
        self.persistor = persistor

        let healthKitManager = HealthKitManager(persistor: persistor)
        self.healthKitManager = healthKitManager

        healthKitManager.dataHandler = self.hkDataHandler
        healthKitManager.requestAccess() { success, error in
            if let error = error {
                completion(.failure(error))
            } else {
                completion(.success(success))
            }
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

    private func hkDataHandler(_ requestData: HKRequestData) {
        self.sendBatch(data: requestData) { result in
            switch result {
            case .success:
                print("SUCESS REQUEST")
            case .failure(let err):
                print("HTTP error:", err)
            }
        }
    }

    private func sendBatch(data: HKRequestData, completion: @escaping (Result<Data, Error>) -> Void) {
        guard let apiClient = self.apiClient else { return  completion(.failure(FjuulError.invalidConfig))}

        let url = "\(apiClient.baseUrl)/sdk/activity-sources/v1/\(apiClient.userToken)/healthkit"

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .formatted(DateFormatters.iso8601Full)
        let parameterEncoder = JSONParameterEncoder(encoder: encoder)

        apiClient.signedSession.request(url, method: .post, parameters: data, encoder: parameterEncoder).response { response in
//            print(response)
//            return completion(response.result)

            return completion(.success(Data()))
        }
    }
}
