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

        refreshSession.request("\(baseUrl)/sdk/signing/v1/issue-key/user", method: .get).validate().responseData { response in
            let decodedResponse = response.tryMap { data -> HmacCredentials in
                let signingKey = try Decoders.iso8601Full.decode(SigningKey.self, from: data)
                return HmacCredentials(signingKey: signingKey)
            }
            completion(decodedResponse.result)
        }

    }

    // Determine based on the request failure if we need to refresh the signing key or if it was a general auth error
    // return true to trigger a key refresh
    func didRequest(_ urlRequest: URLRequest,
                    with response: HTTPURLResponse,
                    failDueToAuthenticationError error: Error) -> Bool {

        if response.statusCode != 401 {
            return false
        }
        return ["expired_signing_key", "invalid_key_id"].contains(response.headers.value(for: "x-authentication-error"))

    }

    // This method should return true if the URLRequest is authenticated in a way that matches the values in the Credential
    // This is used to determine if credentials need to be refreshed, or have already been refreshed (e.g. due to multiple
    // concurrent in-flight requests).
    func isRequest(_ urlRequest: URLRequest, authenticatedWith credential: HmacCredentials) -> Bool {
        guard let signingKey = credential.signingKey, let signature = urlRequest.value(forHTTPHeaderField: "Signature") else {
            return false
        }
        let regex = try? NSRegularExpression(pattern: "keyId=\"([a-f0-9\\-]+)\"", options: .caseInsensitive)
        if let match = regex?.firstMatch(in: signature, range: NSRange(location: 0, length: signature.count)) {
            if let range = Range(match.range(at: 1), in: signature) {
                let keyId = signature[range]
                return keyId == signingKey.id
            }
        }
        return false
    }

}
