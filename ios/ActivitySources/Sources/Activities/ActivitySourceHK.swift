import Foundation
import FjuulCore
import Alamofire

final class ActivitySourceHK: ActivitySourceProtocol {
    static public let shared = ActivitySourceHK()

    var apiClient: ApiClient?
    var persistor: Persistor?

    private init() {}

    func mount(apiClient: ApiClient, persistor: Persistor) -> Bool {
        self.apiClient = apiClient
        self.persistor = persistor

        let healthKitManager = HealthKitManager(persistor: persistor)

        healthKitManager.dataHandler = self.hkDataHandler
        healthKitManager.requestAccess() { success, error in
//            if success { print("HealthKit access granted") }
//            else { print("Error requesting access to HealthKit: \(error)") }
        }

        return true
    }

    func unmount() -> Bool {
        return true
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
    private func mountBackgroundDelivery() {}
    private func unmountBackgroundDelivery() {}

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
