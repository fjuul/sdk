import Foundation
import Alamofire

class HmacAuthenticatior: Authenticator {

    let baseUrl: String
    let refreshSession: Session
    var credentialStore: HmacCredentialStore

    init(baseUrl: String, refreshSession: Session, credentialStore: HmacCredentialStore) {
        self.baseUrl = baseUrl
        self.refreshSession = refreshSession
        self.credentialStore = credentialStore
    }

    func apply(_ credential: SigningKey?, to urlRequest: inout URLRequest) {
        guard let signingKey = credential else {
            return
        }
        urlRequest.signWith(key: signingKey, forDate: Date())
    }

    func refresh(_ credential: SigningKey?,
                 for session: Session,
                 completion: @escaping (Result<SigningKey?, Error>) -> Void) {

        refreshSession.request("\(baseUrl)/sdk/signing/v1/issue-key/user", method: .get).apiResponse { response in
            let decodedResponse = response.tryMap { data -> SigningKey in
                return try Decoders.iso8601Full.decode(SigningKey.self, from: data)
            }
            switch decodedResponse.result {
            case .success(let key):
                self.credentialStore.signingKey = key
                completion(.success(key))
            case .failure(let err):
                completion(.failure(err))
            }
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
        // Note: this is called before our custom error processing in `apiResponse`, thus `error` will not be one of
        // our custom error types and we can not directly use those here
        return ["expired_signing_key", "invalid_key_id"].contains(response.headers.value(for: "x-authentication-error"))

    }

    // This method should return true if the URLRequest is authenticated in a way that matches the values in the Credential
    // This is used to determine if credentials need to be refreshed, or have already been refreshed (e.g. due to multiple
    // concurrent in-flight requests).
    func isRequest(_ urlRequest: URLRequest, authenticatedWith credential: SigningKey?) -> Bool {
        guard let signingKey = credential, let signature = urlRequest.value(forHTTPHeaderField: "Signature") else {
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
