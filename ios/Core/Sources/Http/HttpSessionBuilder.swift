import Foundation
import Alamofire

struct HttpSessionBuilder {

    let baseUrl: String
    let apiKey: String

    func buildBearerAuthenticatedSession(credentials: UserCredentials) -> Session {
        let apiKeyAdapter = ApiKeyAdapter(apiKey: apiKey)
        let bearerAuthAdapter = BearerAuthenticationAdapter(userCredentials: credentials)
        let compositeInterceptor = Interceptor(
            adapters: [apiKeyAdapter, bearerAuthAdapter],
            retriers: [],
            interceptors: []
        )
        return Session(interceptor: compositeInterceptor)
    }

    func buildSignedSession(credentials: UserCredentials) -> Session {

        let hmacCredentials = HmacCredentials(userCredentials: credentials, signingKey: nil)

        let apiKeyAdapter = ApiKeyAdapter(apiKey: apiKey)

        let authenticator = HmacAuthenticationInterceptor()
        let authInterceptor = AuthenticationInterceptor(authenticator: authenticator, credential: hmacCredentials)

        let compositeInterceptor = Interceptor(
            adapters: [apiKeyAdapter],
            retriers: [],
            interceptors: [authInterceptor]
        )

        return Session(interceptor: compositeInterceptor)

    }

}
