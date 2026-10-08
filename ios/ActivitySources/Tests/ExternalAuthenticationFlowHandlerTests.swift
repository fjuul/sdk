import Foundation
import FjuulCore
import XCTest

import FjuulActivitySources

final class ExternalAuthenticationFlowHandlerTests: XCTestCase {

    func testAdditionalQueryParametersPreserveLegacyStatus() {
        for suffix in ["errorCode=oauth_cancelled", "errorCode=google_health_account_not_linked", "errorCode=future_code", "errorCode=", "unrelated=value"] {
            for success in [true, false] {
                let url = URL(string: "fjuulsdk://external_connect?service=googlehealth&success=\(success)&\(suffix)")!
                let status = ExternalAuthenticationFlowHandler.handle(url: url)
                XCTAssertEqual(status.success, success)
                XCTAssertEqual(status.tracker, TrackerValue.GOOGLEHEALTH)
            }
        }
    }

    func testErrorCodeReachesAppFacingStatus() {
        let fixtures: [(String, String?)] = [
            ("", nil),
            ("&errorCode=oauth_cancelled", ExternalAuthenticationFlowHandler.ErrorCode.oauthCancelled),
            ("&errorCode=google_health_account_not_linked", ExternalAuthenticationFlowHandler.ErrorCode.googleHealthAccountNotLinked),
            ("&errorCode=future%5Fcode", "future_code"),
            ("&errorCode=", nil),
            ("&errorCode", nil),
            ("&unrelated=value&error=private_provider_message", nil),
        ]
        for (suffix, expectedCode) in fixtures {
            for success in [true, false] {
                let url = URL(string: "fjuulsdk://external_connect?service=googlehealth&success=\(success)\(suffix)")!
                let appCallback: (ConnectionStatus) -> Void = { status in
                    XCTAssertEqual(status.tracker, TrackerValue.GOOGLEHEALTH)
                    XCTAssertEqual(status.success, success)
                    XCTAssertEqual(status.errorCode, success ? nil : expectedCode)
                }
                deliverBrowserCallback(url: url, completion: appCallback)
            }
        }
    }

    func testExistingStatusConstructionAndMutation() {
        var status = ConnectionStatus(tracker: .POLAR, success: false)
        status.tracker = .GOOGLEHEALTH
        XCTAssertEqual(status.tracker, .GOOGLEHEALTH)
        XCTAssertFalse(status.success)
        XCTAssertNil(status.errorCode)
        XCTAssertNil(ConnectionStatus(success: true).tracker)
        XCTAssertNil(ConnectionStatus(success: true, errorCode: "oauth_cancelled").errorCode)
    }

    func testLocalBrowserDismissalDoesNotReportProviderCancellation() {
        var statuses: [ConnectionStatus] = []
        let appCallback: (ConnectionStatus) -> Void = { statuses.append($0) }
        deliverBrowserCallback(url: nil, completion: appCallback)
        XCTAssertTrue(statuses.isEmpty)

        let url = URL(string: "fjuulsdk://external_connect?service=googlehealth&success=false&errorCode=oauth_cancelled")!
        deliverBrowserCallback(url: url, completion: appCallback)
        XCTAssertEqual(statuses.count, 1)
        XCTAssertFalse(statuses[0].success)
        XCTAssertEqual(statuses[0].errorCode, ExternalAuthenticationFlowHandler.ErrorCode.oauthCancelled)
    }

    private func deliverBrowserCallback(url: URL?, completion: (ConnectionStatus) -> Void) {
        guard let url = url else { return }
        completion(ExternalAuthenticationFlowHandler.handle(url: url))
    }

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
        XCTAssertEqual(connectionStatus.tracker?.value, "unknown")
        XCTAssertNil(connectionStatus.errorCode)
    }
}
