import Foundation
import Alamofire

struct ApiKeyAdapter: RequestAdapter {

    let apiKey: String

    func adapt(_ urlRequest: URLRequest, for session: Session, completion: @escaping (Result<URLRequest, Error>) -> Void) {
        var urlRequest = urlRequest
        urlRequest.headers.add(name: "x-api-key", value: apiKey)
        completion(.success(urlRequest))
    }

}
