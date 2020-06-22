import XCTest
@testable import FjuulAnalytics

final class FjuulAnalyticsTests: XCTestCase {
    func testExample() {
        // This is an example of a functional test case.
        // Use XCTAssert and related functions to verify your tests produce the correct
        // results.
        XCTAssertEqual(Analytics().text(), "Hello, Core!")
    }

    static var allTests = [
        ("testExample", testExample),
    ]
}
