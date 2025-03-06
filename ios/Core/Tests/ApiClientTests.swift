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
    let credentials = UserCredentials(
        token: "b530b31f-74ca-4814-9e24-1bd35d5d1b61",
        secret: "9b28de21-905b-4ff3-8e66-7859e776e143"
    )

    override func setUp() {
        super.setUp()
        stub(condition: isHost("foo")) { _ in
            let stubData = Data("".utf8)
            return HTTPStubsResponse(data: stubData, statusCode: 200, headers: nil)
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

    func testBearerAuthenticationSessionAttachesAuthHeader() {
        let e = expectation(description: "Alamofire")
        let client = ApiClient(baseUrl: "", apiKey: "this-is-sparta", credentials: credentials, persistor: InMemoryPersistor())
        let request = client.bearerAuthenticatedSession.request("https://foo")
        request.apiResponse { _ in
            XCTAssertEqual(
                request.request?.value(forHTTPHeaderField: "Authorization"),
                "Bearer YjUzMGIzMWYtNzRjYS00ODE0LTllMjQtMWJkMzVkNWQxYjYxOjliMjhkZTIxLTkwNWItNGZmMy04ZTY2LTc4NTllNzc2ZTE0Mw=="
            )
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testBearerAuthenticatedSessionAttachesApiKey() {
        let e = expectation(description: "Alamofire")
        let client = ApiClient(baseUrl: "", apiKey: "this-is-sparta", credentials: credentials, persistor: InMemoryPersistor())
        let request = client.bearerAuthenticatedSession.request("https://foo")
        request.apiResponse { _ in
            XCTAssertEqual(request.request?.value(forHTTPHeaderField: "x-api-key"), "this-is-sparta")
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testSignedSessionAttachesApiKey() {
        let e = expectation(description: "Alamofire")
        let client = ApiClient(baseUrl: "https://apibase", apiKey: "this-is-sparta", credentials: credentials, persistor: InMemoryPersistor())
        let request = client.signedSession.request("https://foo")
        request.apiResponse { _ in
            XCTAssertEqual(request.request?.value(forHTTPHeaderField: "x-api-key"), "this-is-sparta")
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testSignedSessionAttachesSignature() {
        let e = expectation(description: "Alamofire")
        let client = ApiClient(baseUrl: "https://apibase", apiKey: "this-is-sparta", credentials: credentials, persistor: InMemoryPersistor())
        let request = client.signedSession.request("https://foo")
        request.apiResponse { _ in
            // Note: it would be a lot of ugly effort to actually verify the signature here, because
            // this code path actually signs with the current point in time as date, so it is hard to
            // predict the signature.
            XCTAssertNotNil(request.request?.value(forHTTPHeaderField: "Signature"))
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

}
