import Foundation
import Alamofire
import FjuulCore

public class ActivitySourceHealthKitAPI {

    let apiClient: ApiClient

    init(apiClient: ApiClient) {
        self.apiClient = apiClient
    }

    private var baseUrl: URL? {
        return URL(string: self.apiClient.baseUrl)?.appendingPathComponent("sdk/activity-sources/v1")
    }

//    // TODO: Continue work on that code after merge https://github.com/fjuul/sdk-server/pull/791
//    public func sendBatches(data: HKRequestData, completion: @escaping (Result<Void, Error>) -> Void) {
//        let path = "/\(apiClient.userToken)/healthkit"
//
//        guard let url = baseUrl?.appendingPathComponent(path) else {
//            return completion(.failure(FjuulError.invalidConfig))
//        }
//
//        apiClient.signedSession.request(url, method: .post, parameters: batches.asJsonEncodableDictionary(), encoding: JSONEncoding.default).apiResponse { response in
//            return completion(response.result)
//        }
//    }

}
