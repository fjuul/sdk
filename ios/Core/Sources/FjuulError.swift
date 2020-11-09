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

    public enum ActivitySourceConnectionFailureReason {
        // TODO actually introduce different failure cases for different failures
        case generic
        case sourceAlreadyConnected
    }

    public enum ActivitySourceDataManageFailure {
        case hkNotAvailableOnDevice
        case activitySourceNotMounted
    }

    case invalidConfig
    case authenticationFailure(reason: AuthenticationFailureReason)

    // TODO this breaks module encapsulation as Core should not have knowledge of ActivitySources internals,
    // however unfortunately it is not possible to extend FjuulError (enum) from another module and still use
    // this as single entrypoint for all possible Errors.
    case activitySourceConnectionFailure(reason: ActivitySourceConnectionFailureReason)
    case activitySourceFailure(reason: ActivitySourceDataManageFailure)

}
