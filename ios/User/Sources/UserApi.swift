import Foundation
import Alamofire
import FjuulCore

/**
 `PartialUserProfile` is a structure in which only a part of the fields of the original UserProfile structure can be filled.

 If you create a new user, you need to fill in all the necessary fields.
 In the case of updating the existing profile, it is enough to fill in only the fields that need to be updated.

 Please keep in mind, that if you want to not lose precision in Decimal numbers with the floating-point (e.g. weight,
 height), then you should initialize such values by a string literal:
 ~~~
 let profile = PartialUserProfile { partial in
    partial[\.height] = Decimal(string: "170.2689")!
    partial[\.weight] = Decimal(string: "60.6481")!
 }
 */
public typealias PartialUserProfile = Partial<UserProfile>

/// The `UserApi` encapsulates access to a users profile data.
public class UserApi {
    private static var userProfileJSONDecoder: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .formatted(DateFormatters.yyyyMMddLocale)
        return decoder
    }

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
                .tryMap { data -> UserCreationResult in
                    let decoder = self.userProfileJSONDecoder
                    let creationResultJson = try JSONSerialization.jsonObject(with: data) as? [String: Any]
                    let userProfileJson = creationResultJson?["user"] as? [String: Any]
                    decoder.userInfo = [UserProfileCodingOptions.key: UserProfileCodingOptions(json: userProfileJson)]
                    return try decoder.decode(UserCreationResult.self, from: data)
                }.mapError { err -> Error in
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
                    let decoder = UserApi.userProfileJSONDecoder
                    let json = try JSONSerialization.jsonObject(with: data)
                    decoder.userInfo = [UserProfileCodingOptions.key: UserProfileCodingOptions(json: json as? [String : Any])]
                    return try decoder.decode(UserProfile.self, from: data)
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
                .tryMap { data -> UserProfile in
                    let decoder = UserApi.userProfileJSONDecoder
                    let json = try JSONSerialization.jsonObject(with: data)
                    decoder.userInfo = [UserProfileCodingOptions.key: UserProfileCodingOptions(json: json as? [String : Any])]
                    return try decoder.decode(UserProfile.self, from: data)
                }
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
