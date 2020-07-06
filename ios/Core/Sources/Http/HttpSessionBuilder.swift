import Foundation
import Alamofire

struct HttpSessionBuilder {

    let baseUrl: String
    let apiKey: String

    let credentials: UserCredentials

    func buildBearerAuthenticatedSession() -> Session {
        let apiKeyAdapter = ApiKeyAdapter(apiKey: apiKey)
        let bearerAuthAdapter = BearerAuthenticationAdapter(userCredentials: credentials)
        let compositeInterceptor = Interceptor(
            adapters: [apiKeyAdapter, bearerAuthAdapter],
            retriers: [],
            interceptors: []
        )
        return Session(interceptor: compositeInterceptor)
    }

    func buildSignedSession() -> Session {

        let hmacCredentials = HmacCredentials(signingKey: nil)

        let apiKeyAdapter = ApiKeyAdapter(apiKey: apiKey)

        let authenticator = HmacAuthenticationInterceptor(refreshSession: self.buildBearerAuthenticatedSession())
        let authInterceptor = AuthenticationInterceptor(authenticator: authenticator, credential: hmacCredentials)

        let compositeInterceptor = Interceptor(
            adapters: [apiKeyAdapter],
            retriers: [],
            interceptors: [authInterceptor]
        )

        let monitor = ClosureEventMonitor()
        monitor.requestDidCompleteTaskWithError = { (request, task, error) in
            debugPrint(request)
        }

        return Session(interceptor: compositeInterceptor, eventMonitors: [monitor])

    }

}
