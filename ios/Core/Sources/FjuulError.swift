import Foundation

public enum FjuulError: Error {

    public enum AuthenticationFailureReason: String {
        case invalidKeyId = "invalid_key_id"
        case expiredSigningKey = "expired_signing_key"
        case mismatchedRequestSignature = "mismatched_request_signature"
        case badSignatureHeader = "bad_signature_header"
        case wrongCredentials = "wrong_credentials"
        case clockSkew = "clock_skew"
    }

    case invalidConfig
    case authenticationFailure(reason: AuthenticationFailureReason)

}
