import Foundation
import Alamofire
import FjuulCore

public typealias PartialUserProfile = Partial<UserProfile>

/// The `UserApi` encapsulates access to a users profile data.
public class UserApi {

    static public func create(baseUrl: String, apiKey: String, profile: PartialUserProfile, completion: @escaping (Result<UserCreationResult, Error>) -> Void) {
        let maybeUrl = URL(string: baseUrl)?.appendingPathComponent("sdk/users/v1/")
        guard let url = maybeUrl else {
            return completion(.failure(FjuulError.invalidConfig))
        }
        var profileData = profile
        if profileData[\.timezone] == nil {
            profileData[\.timezone] = TimeZone.current
        }
        if (profileData[\.locale] ?? "").isEmpty {
            profileData[\.locale] = Bundle.main.preferredLocalizations.first ?? "en"
        }
        ApiClient.requestUnauthenticated(url, apiKey: apiKey, method: .post,
                                         parameters: profileData.asJsonEncodableDictionary(), encoding: JSONEncoding.default).apiResponse { response in
            let decodedResponse = response
                .tryMap { try Decoders.yyyyMMddLocale.decode(UserCreationResult.self, from: $0) }
                .mapError { err -> Error in
                    guard let responseData = response.data else { return err }
                    guard let errorResponse = try? Decoders.iso8601Full.decode(ValidationErrorJSONBodyResponse.self, from: responseData) else { return err }

                    return FjuulError.userFailure(reason: .validation(error: errorResponse))
                }
            return completion(decodedResponse.result)
        }
    }

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
            let decodedResponse = response
                .tryMap { data -> UserProfile  in
                    let decoder = Decoders.yyyyMMddLocale
                    let json = try JSONSerialization.jsonObject(with: data)
                    decoder.userInfo = [UserProfileCodingOptions.key: UserProfileCodingOptions(json: json as? [String : Any])]
                    return try Decoders.yyyyMMddLocale.decode(UserProfile.self, from: data)
                }.mapAPIError { _, jsonError in
                    guard let jsonError = jsonError else { return nil }

                    return .userFailure(reason: .generic(message: jsonError.message))
                }
            completion(decodedResponse.result)
        }
    }

    public func updateProfile(_ profile: PartialUserProfile, completion: @escaping (Result<UserProfile, Error>) -> Void) {
        let path = "/\(apiClient.userToken)"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(FjuulError.invalidConfig))
        }
        apiClient.signedSession.request(url, method: .put, parameters: profile.asJsonEncodableDictionary(), encoding: JSONEncoding.default).apiResponse { response in
            let decodedResponse = response
                .tryMap { try Decoders.yyyyMMddLocale.decode(UserProfile.self, from: $0) }
                .mapError { err -> Error in
                    guard let responseData = response.data else { return err }
                    guard let errorResponse = try? Decoders.iso8601Full.decode(ValidationErrorJSONBodyResponse.self, from: responseData) else { return err }

                    return FjuulError.userFailure(reason: .validation(error: errorResponse))
                }
            completion(decodedResponse.result)
        }
    }

}

private var AssociatedObjectHandle: UInt8 = 0

public extension ApiClient {

    static func createUser(baseUrl: String, apiKey: String, profile: PartialUserProfile, completion: @escaping (Result<UserCreationResult, Error>) -> Void) {
        return UserApi.create(baseUrl: baseUrl, apiKey: apiKey, profile: profile, completion: completion)
    }

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
