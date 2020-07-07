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

    func testPostRequestWithoutBody() throws {

        var request = try URLRequest(url: "https://fjuul.dev.api/analytics/v1/dailyStats/userToken/2020-01-15", method: .post)
        request.signWith(key: signingKey, forDate: fixedDate)

        XCTAssertEqual(
            request.value(forHTTPHeaderField: "Signature"),
            "keyId=\"signing-key-id-1234\",algorithm=\"hmac-sha256\",headers=\"(request-target) date digest\",signature=\"Mmyp9dkZcBG/7Bk3okAExqvKS/E7bAOyanfAbrdAUnA=\""
        )

        XCTAssertEqual(request.value(forHTTPHeaderField: "Date"), "Thu, 13 Feb 2020 15:56:23 GMT")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Digest"), "")

    }

    func testPostRequestWithJsonBody() throws {

        var request = try URLRequest(url: "https://fjuul.dev.api/analytics/v1/dailyStats/userToken/2020-01-15", method: .post)
        request.httpBody = "{\"hello\":\"world\",\"foo\":\"bar\"}".data(using: String.Encoding.utf8)
        request.signWith(key: signingKey, forDate: fixedDate)

        XCTAssertEqual(
            request.value(forHTTPHeaderField: "Signature"),
            "keyId=\"signing-key-id-1234\",algorithm=\"hmac-sha256\",headers=\"(request-target) date digest\",signature=\"78ygswe54lAGd24/ksNjNXuZ9JNrMTI4E9TsqHaLjaU=\""
        )

        XCTAssertEqual(request.value(forHTTPHeaderField: "Date"), "Thu, 13 Feb 2020 15:56:23 GMT")

        XCTAssertEqual(
            request.value(forHTTPHeaderField: "Digest"),
            "SHA-256=Q95/OQtk+2T6qHbUBZyTr/JITn+2qDMFeqAKJee0Uz0="
        )

    }

}
