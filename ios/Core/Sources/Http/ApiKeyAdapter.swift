import Foundation
import Alamofire

class ApiKeyAdapter: RequestAdapter {

    let apiKey: String

    init(apiKey: String) {
        self.apiKey = apiKey
    }

    func adapt(_ urlRequest: URLRequest, for session: Session, completion: @escaping (Result<URLRequest, Error>) -> Void) {
        var urlRequest = urlRequest
        urlRequest.headers.add(name: "x-api-key", value: apiKey)
        completion(.success(urlRequest))
    }

}
