import Foundation
import Alamofire

/// The `ApiClient` is the central unified entrypoint for all of the functionality provided by the Fjuul SDK.
public class ApiClient {

    /// Creates an unauthenticated request and attaches the provided API key.
    /// This is a helper method currently only required for user creation.
    /// **This is for internal use only.**
    /// - Parameters:
    ///   - convertible:     `URLConvertible` value to be used as the `URLRequest`'s `URL`.
    ///   - apiKey:           The API key to use for the request.
    ///   - method:          `HTTPMethod` for the `URLRequest`. `.get` by default.
    ///   - parameters:      `Parameters` (a.k.a. `[String: Any]`) value to be encoded into the `URLRequest`. `nil` by
    ///                      default.
    ///   - encoding:        `ParameterEncoding` to be used to encode the `parameters` value into the `URLRequest`.
    ///                      `URLEncoding.default` by default.
    /// - Returns: The created `DataRequest`.
    public static func requestUnauthenticated(_ convertible: URLConvertible,
                                              apiKey: String,
                                              method: HTTPMethod = .get,
                                              parameters: Parameters? = nil,
                                              encoding: ParameterEncoding = URLEncoding.default) -> DataRequest {

        let apiKeyAdapter = ApiKeyAdapter(apiKey: apiKey)
        let compositeInterceptor = Interceptor(
            adapters: [apiKeyAdapter],
            retriers: [],
            interceptors: []
        )
        return AF.request(convertible, method: method, parameters: parameters, encoding: encoding, interceptor: compositeInterceptor)

    }

    /// An HTTP session which applies bearer authentication to all requests.
    /// **This is for internal use only.**
    public let bearerAuthenticatedSession: Session

    /// An HTTP session which applies HMAC authentication to all requests.
    /// **This is for internal use only.**
    public let signedSession: Session

    /// The API base URL this API client was initialized with.
    public let baseUrl: String

    public let persistor: Persistor

    let credentials: UserCredentials

    /// Initializes a Fjuul API client.
    ///
    /// - Parameters:
    ///   - baseUrl: The API base URL to connect to, e.g. `https://api.fjuul.com`.
    ///   - apiKey: The API key.
    ///   - credentials: The credentials of the user.
    public convenience init(baseUrl: String, apiKey: String, credentials: UserCredentials) {
        self.init(baseUrl: baseUrl, apiKey: apiKey, credentials: credentials, persistor: DiskPersistor())
    }

    public init(baseUrl: String, apiKey: String, credentials: UserCredentials, persistor: Persistor) {
        self.baseUrl = baseUrl
        self.credentials = credentials
        self.persistor = persistor
        self.bearerAuthenticatedSession = ApiClient.buildBearerAuthenticatedSession(apiKey: apiKey, credentials: credentials)
        self.signedSession = ApiClient.buildSignedSession(
            apiKey: apiKey,
            baseUrl: baseUrl,
            refreshSession: self.bearerAuthenticatedSession,
            credentialStore: HmacCredentialStore(userToken: credentials.token, persistor: persistor)
        )
    }

    public var userToken: String {
        return credentials.token
    }

    /**
     Deletes the stored user file of the shared preferences created internally for persisting the state of Fjuul SDK.
     Ideally, you should not use this method until you actually decide to clear all user data, as the repeated storage clearance will lead to data being unnecessarily
     uploaded/downloaded multiple times.
    
     Keep in mind that if you want to fully reset SDK to the default state, then you need also to disable all backgroundDelivery observers for
     HealthKitActivitySource by call ActivitySourcesManager.unmout.
     
     ~~~
     apiClient.clearPersistentStorage()
     apiClient.activitySourcesManager?.unmout { result in
         switch result {
         case .success:
             apiClient = nil
         case .failure(let err):
             self.error = err
         }
     }
     ~~~
     */
    /// - Returns: Bool which indicates the success of the operation
    public func clearPersistentStorage() -> Bool {
        return type(of: self.persistor).remove(matchKey: self.userToken)
    }

}

fileprivate extension ApiClient {

    static func buildBearerAuthenticatedSession(apiKey: String, credentials: UserCredentials) -> Session {
        let apiKeyAdapter = ApiKeyAdapter(apiKey: apiKey)
        let bearerAuthAdapter = BearerAuthenticationAdapter(userCredentials: credentials)
        let userAgentAdapter = UserAgentAdapter(sdkVersion: FjuulSDKVersion.version)
        let compositeInterceptor = Interceptor(
            adapters: [apiKeyAdapter, bearerAuthAdapter, userAgentAdapter],
            retriers: [],
            interceptors: []
        )

        let configuration = URLSessionConfiguration.af.default
        configuration.urlCache = nil
        return Session(configuration: configuration, interceptor: compositeInterceptor)
    }

    static func buildSignedSession(apiKey: String, baseUrl: String, refreshSession: Session, credentialStore: HmacCredentialStore) -> Session {

        let apiKeyAdapter = ApiKeyAdapter(apiKey: apiKey)
        let userAgentAdapter = UserAgentAdapter(sdkVersion: FjuulSDKVersion.version)

        let authenticator = HmacAuthenticatior(baseUrl: baseUrl, refreshSession: refreshSession, credentialStore: credentialStore)
        let authInterceptor = AuthenticationInterceptor(authenticator: authenticator, credential: credentialStore.signingKey)

        let compositeInterceptor = Interceptor(
            adapters: [apiKeyAdapter, userAgentAdapter],
            retriers: [],
            interceptors: [authInterceptor]
        )

        let configuration = URLSessionConfiguration.af.default
        configuration.urlCache = nil
        return Session(configuration: configuration, interceptor: compositeInterceptor, eventMonitors: [])
    }
}
