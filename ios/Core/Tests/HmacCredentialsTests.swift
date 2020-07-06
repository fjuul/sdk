import XCTest
@testable import FjuulCore

final class HmacCredentialsTests: XCTestCase {

    let userCredentials = UserCredentials(token: "", secret: "")

    func testRequiresRefreshExpired() {
        let key = SigningKey(
            id: "",
            secret: "",
            expiresAt: Date(timeIntervalSinceNow: 60 * -1)
        )
        let credentials = HmacCredentials(userCredentials: userCredentials, signingKey: key)
        XCTAssertTrue(credentials.requiresRefresh)
    }

    func testRequiresRefreshNotExpired() {
        let key = SigningKey(
            id: "",
            secret: "",
            expiresAt: Date(timeIntervalSinceNow: 60 * 6)
        )
        let credentials = HmacCredentials(userCredentials: userCredentials, signingKey: key)
        XCTAssertFalse(credentials.requiresRefresh)
    }

    func testNoSigningKey() {
        let credentials = HmacCredentials(userCredentials: userCredentials, signingKey: nil)
        XCTAssertTrue(credentials.requiresRefresh)
    }

}
