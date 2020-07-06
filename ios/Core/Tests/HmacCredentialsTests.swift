import XCTest
@testable import FjuulCore

final class HmacCredentialsTests: XCTestCase {

    func testRequiresRefreshExpired() {
        let key = SigningKey(
            id: "",
            secret: "",
            expiresAt: Date(timeIntervalSinceNow: 60 * -1)
        )
        let credentials = HmacCredentials(signingKey: key)
        XCTAssertTrue(credentials.requiresRefresh)
    }

    func testRequiresRefreshNotExpired() {
        let key = SigningKey(
            id: "",
            secret: "",
            expiresAt: Date(timeIntervalSinceNow: 60 * 6)
        )
        let credentials = HmacCredentials(signingKey: key)
        XCTAssertFalse(credentials.requiresRefresh)
    }

    func testNoSigningKey() {
        let credentials = HmacCredentials(signingKey: nil)
        XCTAssertTrue(credentials.requiresRefresh)
    }

}
