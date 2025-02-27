import Foundation
import FjuulCore
import XCTest

@testable import FjuulActivitySources

final class ExternalAuthenticationFlowHandlerTests: XCTestCase {

    func testHandleValidUrlWithSuccessStatus() {
        let url = URL(string: "fjuulsdk-exampleapp://externalConnection?service=polar&success=true")

        let connectionStatus = ExternalAuthenticationFlowHandler.handle(url: url!)

        XCTAssert(connectionStatus.success, "Wrong connection status")
        XCTAssertEqual(connectionStatus.tracker, TrackerValue.POLAR)
    }

    func testHandleValidUrlWithSuccessFail() {
        let url = URL(string: "fjuulsdk-exampleapp://externalConnection?service=polar&success=false")

        let connectionStatus = ExternalAuthenticationFlowHandler.handle(url: url!)

        XCTAssert(!connectionStatus.success, "Wrong connection status")
        XCTAssertEqual(connectionStatus.tracker, TrackerValue.POLAR)
    }

    func testHandleInValidUrl() {
        let url = URL(string: "fjuulsdk-exampleapp://externalConnection")

        let connectionStatus = ExternalAuthenticationFlowHandler.handle(url: url!)

        XCTAssert(!connectionStatus.success, "Wrong connection status")
        XCTAssertEqual(connectionStatus.tracker, TrackerValue(value: "unknown"))
    }
}
