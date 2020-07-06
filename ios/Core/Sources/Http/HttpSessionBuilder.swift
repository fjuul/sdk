import Foundation
import Alamofire

struct HttpSessionBuilder {

    let baseUrl: String
    let apiKey: String

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

        let session = Session(interceptor: compositeInterceptor)
        return session

    }

}
