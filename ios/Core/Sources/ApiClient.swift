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
    let persistor: Persistor

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
        return Session(configuration: configuration, interceptor: compositeInterceptor)

    }

}
