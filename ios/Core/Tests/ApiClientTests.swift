import XCTest
import OHHTTPStubs
import OHHTTPStubsSwift
@testable import FjuulCore

final class ApiClientTests: XCTestCase {

    let signingKeyResponse = """
        {
            \"id\":\"d8ad4ea1-fff3-43d0-b4d4-7d007b3ee9ad\",
            \"secret\":\"bbLXRlZ0tN1uriURxNaaajwWPsTfVrvY408vFanPQDE=\",
            \"expiresAt\":\"2030-01-01T00:00:00.000Z\"
        }
    """
    let credentials = UserCredentials(token: "", secret: "")

    override func setUp() {
        super.setUp()
        stub(condition: isHost("foo")) { _ in
            let stubData = "".data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }
        stub(condition: isHost("apibase") && isPath("/sdk/signing/v1/issue-key/user")) { _ in
            let stubData = self.signingKeyResponse.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }
    }

    override func tearDown() {
        HTTPStubs.removeAllStubs()
        super.tearDown()
    }

    func testBearerAuthenticatedSessionAttachesApiKey() {
        let e = expectation(description: "Alamofire")
        let client = ApiClient(baseUrl: "", apiKey: "this-is-sparta", credentials: credentials)
        let request = client.bearerAuthenticatedSession.request("https://foo")
        request.response { _ in
            XCTAssertEqual(request.request?.value(forHTTPHeaderField: "x-api-key"), "this-is-sparta")
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }
    
    func testSignedSessionAttachesApiKey() {
        let e = expectation(description: "Alamofire")
        let client = ApiClient(baseUrl: "https://apibase", apiKey: "this-is-sparta", credentials: credentials)
        let request = client.signedSession.request("https://foo")
        request.response { _ in
            XCTAssertEqual(request.request?.value(forHTTPHeaderField: "x-api-key"), "this-is-sparta")
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

}
