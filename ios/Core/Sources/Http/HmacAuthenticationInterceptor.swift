import Foundation
import Alamofire

class HmacAuthenticationInterceptor: Authenticator {

    let baseUrl: String
    let refreshSession: Session

    init(baseUrl: String, refreshSession: Session) {
        self.baseUrl = baseUrl
        self.refreshSession = refreshSession
    }

    func apply(_ credential: HmacCredentials, to urlRequest: inout URLRequest) {
        guard let signingKey = credential.signingKey else {
            // TODO this should never happen
            return
        }
        urlRequest.signWith(key: signingKey, forDate: Date())
    }

    func refresh(_ credential: HmacCredentials,
                 for session: Session,
                 completion: @escaping (Result<HmacCredentials, Error>) -> Void) {

        refreshSession.request("\(baseUrl)/sdk/signing/v1/issue-key/user", method: .get)
            .validate(statusCode: 200..<299)
            .response { response in
                let decodedResponse = response.tryMap { (data: Data?) -> HmacCredentials in
                    let decoder = JSONDecoder()
                    decoder.dateDecodingStrategy = .formatted(DateFormatters.iso8601Full)
                    let signingKey = try decoder.decode(SigningKey.self, from: data!)
                    return HmacCredentials(signingKey: signingKey)
                }
                completion(decodedResponse.result)
            }

    }

    func didRequest(_ urlRequest: URLRequest,
                    with response: HTTPURLResponse,
                    failDueToAuthenticationError error: Error) -> Bool {
        // If authentication server CANNOT invalidate credentials, return `false`
        return false

        // If authentication server CAN invalidate credentials, then inspect the response matching against what the
        // authentication server returns as an authentication failure. This is generally a 401 along with a custom
        // header value.
        // return response.statusCode == 401
    }

    func isRequest(_ urlRequest: URLRequest, authenticatedWith credential: HmacCredentials) -> Bool {
        // If authentication server CANNOT invalidate credentials, return `true`
        return true

        // If authentication server CAN invalidate credentials, then compare the "Authorization" header value in the
        // `URLRequest` against the Bearer token generated with the access token of the `Credential`.
        // let bearerToken = HTTPHeader.authorization(bearerToken: credential.accessToken).value
        // return urlRequest.headers["Authorization"] == bearerToken
    }

}
