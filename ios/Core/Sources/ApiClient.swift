import Foundation
import Alamofire

/// The `ApiClient` is the central unified entrypoint for all of the functionality provided by the Fjuul SDK.
public class ApiClient {

    /// An HTTP session which applies bearer authentication to all requests.
    /// This is for internal use only.
    public let bearerAuthenticatedSession: Session

    /// An HTTP session which applies HMAC authentication to all requests.
    /// This is for internal use only.
    public let signedSession: Session

    /// The API base URL this API client was initialized with.
    public let baseUrl: String

    let credentials: UserCredentials

    /// Initializes a Fjuul API client.
    ///
    /// - Parameters:
    ///   - baseUrl: The API base URL to connect to, e.g. `https://api.fjuul.com`.
    ///   - apiKey: The API key.
    ///   - credentials: The credentials of the user.
    public init(baseUrl: String, apiKey: String, credentials: UserCredentials) {
        self.baseUrl = baseUrl
        self.credentials = credentials
        self.bearerAuthenticatedSession = ApiClient.buildBearerAuthenticatedSession(apiKey: apiKey, credentials: credentials)
        self.signedSession = ApiClient.buildSignedSession(apiKey: apiKey, baseUrl: baseUrl, refreshSession: self.bearerAuthenticatedSession)
    }

    public var userToken: String {
        return credentials.token
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
        return Session(interceptor: compositeInterceptor)
    }

    static func buildSignedSession(apiKey: String, baseUrl: String, refreshSession: Session) -> Session {

        let hmacCredentials = HmacCredentials(signingKey: nil)

        let apiKeyAdapter = ApiKeyAdapter(apiKey: apiKey)

        let authenticator = HmacAuthenticationInterceptor(baseUrl: baseUrl, refreshSession: refreshSession)
        let authInterceptor = AuthenticationInterceptor(authenticator: authenticator, credential: hmacCredentials)

        let compositeInterceptor = Interceptor(
            adapters: [apiKeyAdapter],
            retriers: [],
            interceptors: [authInterceptor]
        )

        return Session(interceptor: compositeInterceptor)

    }

}