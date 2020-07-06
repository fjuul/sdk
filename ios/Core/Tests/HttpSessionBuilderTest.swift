import XCTest
import OHHTTPStubs
import OHHTTPStubsSwift
@testable import FjuulCore

final class HttpSessionBuilderTests: XCTestCase {

    override func setUp() {
        super.setUp()
        stub(condition: { _ in true }) { _ in
            let stubData = "".data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }
    }

    override func tearDown() {
        HTTPStubs.removeAllStubs()
        super.tearDown()
    }

    func testAttachesApiKey() {
        let e = expectation(description: "Alamofire")
        let builder = HttpSessionBuilder(baseUrl: "https://foo", apiKey: "this-is-sparta")
        let session = builder.buildBearerAuthenticatedSession(credentials: UserCredentials(token: "", secret: ""))
        let request = session.request("https://foo")
        request.response { _ in
            XCTAssertEqual(request.request?.value(forHTTPHeaderField: "x-api-key"), "this-is-sparta")
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

}
