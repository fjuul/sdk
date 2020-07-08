import Foundation
import Alamofire

public class ApiClient {

    public let bearerAuthenticatedSession: Session
    public let signedSession: Session
    public let baseUrl: String

    let credentials: UserCredentials

    public init(baseUrl: String, apiKey: String, credentials: UserCredentials) {
        self.baseUrl = baseUrl
        self.credentials = credentials
        self.bearerAuthenticatedSession = ApiClient.buildBearerAuthenticatedSession(apiKey: apiKey, credentials: credentials)
        self.signedSession = ApiClient.buildSignedSession(apiKey: apiKey, baseUrl: baseUrl, refreshSession: self.bearerAuthenticatedSession)
    }

    public var userToken: String {
        get { return credentials.token }
    }

}

extension ApiClient {

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
