import Foundation
import Alamofire

struct BearerAuthenticationAdapter: RequestAdapter {

    let userCredentials: UserCredentials

    func adapt(_ urlRequest: URLRequest, for session: Session, completion: @escaping (Result<URLRequest, Error>) -> Void) {
        var urlRequest = urlRequest
        urlRequest.headers.add(.authorization(userCredentials.completeAuthString()))
        completion(.success(urlRequest))
    }

}
