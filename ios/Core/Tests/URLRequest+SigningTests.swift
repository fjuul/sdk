import XCTest
@testable import FjuulCore

final class URLRequestSiginigTests: XCTestCase {

    let signingKey = SigningKey(
        id: "signing-key-id-1234",
        secret: "REAL_SECRET_KEY",
        expiresAt: Date.distantFuture
    )

    let fixedDate = Date(timeIntervalSince1970: 1581609383)

    func testSimpleGetRequest() throws {

        var request = try URLRequest(url: "https://fjuul.dev.api/analytics/v1/dailyStats/userToken/2020-01-15", method: .get)
        request.signWith(key: signingKey, forDate: fixedDate)

        XCTAssertEqual(
            request.value(forHTTPHeaderField: "Signature"),
            "keyId=\"signing-key-id-1234\",algorithm=\"hmac-sha256\",headers=\"(request-target) date\",signature=\"tu8E+96kyaexTmJ7Oep4Ds4bDFYE5ZdDWafqS8yEd20=\""
        )

        XCTAssertEqual(request.value(forHTTPHeaderField: "Date"), "Thu, 13 Feb 2020 15:56:23 GMT")

    }

}
