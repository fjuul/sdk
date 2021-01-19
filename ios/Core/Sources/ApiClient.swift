import Foundation
import Alamofire

/// The `ApiClient` is the central unified entrypoint for all of the functionality provided by the Fjuul SDK.
open class ApiClient {

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

    // TODO: Add docs
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
    
    /// Deletes the stored user file of the shared preferences created internally for persisting the state of Fjuul SDK.
    /// Note that if you want to perform the logout, then you need also to disable all backgroundDelivery observers for ActivitySourceHK
    /// - Returns: Bool which indicates the success of the operation
    public func clearPersistentStorage() -> Bool {
        return type(of: self.persistor).remove(matchKey: self.userToken)
    }

}

fileprivate extension ApiClient {

    static func buildBearerAuthenticatedSession(apiKey: String, credentials: UserCredentials) -> Session {
        let apiKeyAdapter = ApiKeyAdapter(apiKey: apiKey)
        let bearerAuthAdapter = BearerAuthenticationAdapter(userCredentials: credentials)
        let compositeInterceptor = Interceptor(
            adapters: [apiKeyAdapter, bearerAuthAdapter],
            retriers: [],
            interceptors: []
        )
        let configuration = URLSessionConfiguration.af.default
        configuration.urlCache = nil
        return Session(configuration: configuration, interceptor: compositeInterceptor)
    }

    static func buildSignedSession(apiKey: String, baseUrl: String, refreshSession: Session, credentialStore: HmacCredentialStore) -> Session {

        let apiKeyAdapter = ApiKeyAdapter(apiKey: apiKey)

        let authenticator = HmacAuthenticatior(baseUrl: baseUrl, refreshSession: refreshSession, credentialStore: credentialStore)
        let authInterceptor = AuthenticationInterceptor(authenticator: authenticator, credential: credentialStore.signingKey)

        let compositeInterceptor = Interceptor(
            adapters: [apiKeyAdapter],
            retriers: [],
            interceptors: [authInterceptor]
        )

        let configuration = URLSessionConfiguration.af.default
        configuration.urlCache = nil
        return Session(configuration: configuration, interceptor: compositeInterceptor, eventMonitors: [ AlamofireLogger() ])
    }
}

final class AlamofireLogger: EventMonitor {

//    func requestDidResume(_ request: Request) {
//
//        let allHeaders = request.request.flatMap { $0.allHTTPHeaderFields.map { $0.description } } ?? "None"
//        let headers = """
//        ⚡️⚡️⚡️⚡️ Request Started: \(request)
//        ⚡️⚡️⚡️⚡️ Headers: \(allHeaders)
//        """
//        NSLog(headers)
//
//        let body = request.request.flatMap { $0.httpBody.map { String(decoding: $0, as: UTF8.self) } } ?? "None"
//        let message = """
//        ⚡️⚡️⚡️⚡️ Request Started: \(request)
//        ⚡️⚡️⚡️⚡️ Body Data: \(body)
//        """
//        print(message)
//    }
//
//    func request<Value>(_ request: DataRequest, didParseResponse response: AFDataResponse<Value>) {
//
//        NSLog("⚡️⚡️⚡️⚡️ Response Received: \(response.debugDescription)")
//        NSLog("⚡️⚡️⚡️⚡️ Response All Headers: \(String(describing: response.response?.allHeaderFields))")
//    }
}
