import Foundation
import FjuulCore
import XCTest

@testable import FjuulActivitySources

final class ExternalAuthenticationFlowHandlerTests: XCTestCase {
    override func setUp() {
        super.setUp()
    }

    override func tearDown() {
        super.tearDown()
    }

    func testHandleValidUrlWithSuccessStatus() {
        let url = URL(string: "fjuulsdk-exampleapp://externalConnection?service=polar&success=true")

        let connectionStatus = ExternalAuthenticationFlowHandler.handle(url: url!)

        XCTAssert(connectionStatus.success, "Wrong connection status")
        XCTAssertEqual(connectionStatus.tracker, ActivitySourcesItem.polar)
    }

    func testHandleValidUrlWithSuccessFail() {
        let url = URL(string: "fjuulsdk-exampleapp://externalConnection?service=polar&success=false")

        let connectionStatus = ExternalAuthenticationFlowHandler.handle(url: url!)

        XCTAssert(!connectionStatus.success, "Wrong connection status")
        XCTAssertEqual(connectionStatus.tracker, ActivitySourcesItem.polar)
    }

    func testHandleInValidUrl() {
        let url = URL(string: "fjuulsdk-exampleapp://externalConnection")

        let connectionStatus = ExternalAuthenticationFlowHandler.handle(url: url!)

        XCTAssert(!connectionStatus.success, "Wrong connection status")
        XCTAssertNil(connectionStatus.tracker, "Wrong tracker")
    }
}
