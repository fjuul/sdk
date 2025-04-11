import Foundation
import Alamofire

/// Alamofire adapter for adding User-Agent header to requests
final class UserAgentAdapter: RequestAdapter {
    let sdkVersion: String

    init(sdkVersion: String) {
        self.sdkVersion = sdkVersion
    }

    func adapt(_ urlRequest: URLRequest, for session: Session, completion: @escaping (Result<URLRequest, Error>) -> Void) {
        var urlRequest = urlRequest
        urlRequest.headers.add(.userAgent("Fjuul-iOS-SDK/\(sdkVersion)"))
        completion(.success(urlRequest))
    }
}
