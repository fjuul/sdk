import Foundation
import Alamofire
import FjuulCore

public enum ConnectionResult {
    case externalAuthenticationFlowRequired(authenticationUrl: String)
    case connected
}

/// The `ActivitySourcesApi` encapsulates the management of a users activity sources.
public class ActivitySourcesApi {

    let apiClient: ApiClient

    /// Initializes an `ActivitySourcesApi` instance.
    ///
    /// You should generally not call this directly, but instead use the default instance provided on `ApiClient`.
    /// - Parameter apiClient: The `ApiClient` instance to use for API requests.
    init(apiClient: ApiClient) {
        self.apiClient = apiClient
    }

    private var baseUrl: URL? {
        return URL(string: self.apiClient.baseUrl)?.appendingPathComponent("sdk/activity-sources/v1")
    }

    // TODO this should take a higher-level input, not a string source name
    public func connect(activitySource: String, completion: @escaping (Result<ConnectionResult, Error>) -> Void) {
        let path = "/\(apiClient.userToken)/connections/\(activitySource)"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(FjuulError.invalidConfig))
        }
        apiClient.signedSession.request(url, method: .post).apiResponse { response in
            let mappedResponse = response
                .tryMap { result -> ConnectionResult in
                    switch response.response?.statusCode {
                    case 200:
                        let json = try JSONSerialization.jsonObject(with: result, options: []) as? [String: Any]
                        guard let authenticationUrl = json?["url"] as? String else {
                            throw FjuulError.activitySourceConnectionFailure(reason: .generic)
                        }
                        return .externalAuthenticationFlowRequired(authenticationUrl: authenticationUrl)
                    // TODO map to Connection entity and attach that to .connected case
                    case 201: return .connected
                    default: throw FjuulError.activitySourceConnectionFailure(reason: .generic)
                    }
                }
                .mapError { err -> Error in
                    if response.response?.statusCode == 409 {
                        return FjuulError.activitySourceConnectionFailure(reason: .sourceAlreadyConnected)
                    }
                    return err
                }
            completion(mappedResponse.result)
        }
    }

}

private var AssociatedObjectHandle: UInt8 = 0

public extension ApiClient {

    var activitySources: ActivitySourcesApi {
        if let activitySourcesApi = objc_getAssociatedObject(self, &AssociatedObjectHandle) as? ActivitySourcesApi {
            return activitySourcesApi
        } else {
            let activitySourcesApi = ActivitySourcesApi(apiClient: self)
            objc_setAssociatedObject(self, &AssociatedObjectHandle, activitySourcesApi, .OBJC_ASSOCIATION_RETAIN)
            return activitySourcesApi
        }
    }

}
