import Foundation
import Alamofire

final class BearerAuthenticationAdapter: RequestAdapter {

    let userCredentials: UserCredentials

    init(userCredentials: UserCredentials) {
        self.userCredentials = userCredentials
    }

    func adapt(_ urlRequest: URLRequest, for session: Session, completion: @escaping (Result<URLRequest, Error>) -> Void) {
        var urlRequest = urlRequest
        urlRequest.headers.add(.authorization(userCredentials.completeAuthString()))
        completion(.success(urlRequest))
    }

}
