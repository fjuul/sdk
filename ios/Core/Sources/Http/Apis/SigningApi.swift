import Foundation
import Alamofire

enum SigningApi: URLRequestConvertible {

    case issueUserKey

    var baseURL: URL {
        return URL(string: "https://dev.api.fjuul.com/sdk/signing/v1")!
    }

    var method: HTTPMethod {
        switch self {
        case .issueUserKey: return .get
        }
    }

    var path: String {
        switch self {
        case .issueUserKey: return "/issue-key/user"
        }
    }

    func asURLRequest() throws -> URLRequest {
        let url = baseURL.appendingPathComponent(path)
        var request = URLRequest(url: url)
        request.method = method
        return request
    }

}
