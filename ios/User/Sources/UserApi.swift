import Foundation
import Alamofire
import FjuulCore

/// The `UserApi` encapsulates access to a users profile data.
public class UserApi {

    let apiClient: ApiClient

    /// Initializes an `UserApi` instance.
    ///
    /// You should generally not call this directly, but instead use the default instance provided on `ApiClient`.
    /// - Parameter apiClient: The `ApiClient` instance to use for API requests.
    init(apiClient: ApiClient) {
        self.apiClient = apiClient
    }

    private var baseUrl: URL? {
        return URL(string: self.apiClient.baseUrl)?.appendingPathComponent("sdk/users/v1")
    }

    public func getProfile(completion: @escaping (Result<UserProfile, Error>) -> Void) {
        let path = "/\(apiClient.userToken)"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(FjuulError.invalidConfig))
        }
        apiClient.signedSession.request(url, method: .get).apiResponse { response in
            let decodedResponse = response.tryMap { try Decoders.yyyyMMdd.decode(UserProfile.self, from: $0) }
            completion(decodedResponse.result)
        }
    }

}

private var AssociatedObjectHandle: UInt8 = 0

public extension ApiClient {

    var user: UserApi {
        if let userApi = objc_getAssociatedObject(self, &AssociatedObjectHandle) as? UserApi {
            return userApi
        } else {
            let userApi = UserApi(apiClient: self)
            objc_setAssociatedObject(self, &AssociatedObjectHandle, userApi, .OBJC_ASSOCIATION_RETAIN)
            return userApi
        }
    }

}
