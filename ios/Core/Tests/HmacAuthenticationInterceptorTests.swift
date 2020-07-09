import XCTest
import Alamofire
@testable import FjuulCore

final class HmacAuthenticationInterceptorTests: XCTestCase {

    let key = SigningKey(
        id: "0b12b123-38f2-410c-b770-82a4f8bc6001",
        secret: "secret",
        expiresAt: Date.distantFuture
    )

    let url = URL(string: "https://foo")!

    func testIsAuthenticatedNoCredentialsNoSignature() {
        let credentials = HmacCredentials(signingKey: nil)
        let interceptor = HmacAuthenticationInterceptor(baseUrl: "https://foo", refreshSession: Session())
        let request = URLRequest(url: url)
        XCTAssertFalse(interceptor.isRequest(request, authenticatedWith: credentials))
    }

    func testIsAuthenticatedWithCredentialsNoSignature() {
        let credentials = HmacCredentials(signingKey: key)
        let interceptor = HmacAuthenticationInterceptor(baseUrl: "https://foo", refreshSession: Session())
        let request = URLRequest(url: url)
        XCTAssertFalse(interceptor.isRequest(request, authenticatedWith: credentials))
    }

    func testIsAuthenticatedWithCredentialsInvalidKey() {
        let credentials = HmacCredentials(signingKey: key)
        let interceptor = HmacAuthenticationInterceptor(baseUrl: "https://foo", refreshSession: Session())
        var request = URLRequest(url: url)
        request.addValue(
            "keyId=\"other-key-id\",algorithm=\"hmac-sha256\",headers=\"(request-target) date\",signature=\"ops3L4iYL3YI1mrQ6HJbrNPuYL7av1lkGHluPuuaZig=\"",
            forHTTPHeaderField: "Signature"
        )
        XCTAssertFalse(interceptor.isRequest(request, authenticatedWith: credentials))
    }

    func testIsAuthenticatedWithCredentialsWrongKey() {
        let credentials = HmacCredentials(signingKey: key)
        let interceptor = HmacAuthenticationInterceptor(baseUrl: "https://foo", refreshSession: Session())
        var request = URLRequest(url: url)
        request.addValue(
            "keyId=\"7ac24e53-95a4-479f-a9c5-96c778d5df1d\",algorithm=\"hmac-sha256\",headers=\"(request-target) date\",signature=\"ops3L4iYL3YI1mrQ6HJbrNPuYL7av1lkGHluPuuaZig=\"",
            forHTTPHeaderField: "Signature"
        )
        XCTAssertFalse(interceptor.isRequest(request, authenticatedWith: credentials))
    }

    func testIsAuthenticatedWithCredentialsSameKey() {
        let credentials = HmacCredentials(signingKey: key)
        let interceptor = HmacAuthenticationInterceptor(baseUrl: "https://foo", refreshSession: Session())
        var request = URLRequest(url: url)
        request.addValue(
            "keyId=\"0b12b123-38f2-410c-b770-82a4f8bc6001\",algorithm=\"hmac-sha256\",headers=\"(request-target) date\",signature=\"ops3L4iYL3YI1mrQ6HJbrNPuYL7av1lkGHluPuuaZig=\"",
            forHTTPHeaderField: "Signature"
        )
        XCTAssertTrue(interceptor.isRequest(request, authenticatedWith: credentials))
    }

}
