import XCTest
@testable import FjuulCore

final class SigningKeyTests: XCTestCase {

    func testRequiresRefreshExpired() {
        let key = SigningKey(
            id: "",
            secret: "",
            expiresAt: Date(timeIntervalSinceNow: 60 * -1)
        )
        XCTAssertTrue(key.requiresRefresh)
    }

    func testRequiresRefreshNotExpired() {
        let key = SigningKey(
            id: "",
            secret: "",
            expiresAt: Date(timeIntervalSinceNow: 60 * 6)
        )
        XCTAssertFalse(key.requiresRefresh)
    }

}
