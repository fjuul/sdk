import XCTest
@testable import FjuulCore

final class FjuulCoreTests: XCTestCase {
    func testExample() {
        // This is an example of a functional test case.
        // Use XCTAssert and related functions to verify your tests produce the correct
        // results.
        XCTAssertEqual(Core().text, "Hello, Core!")
    }

    static var allTests = [
        ("testExample", testExample),
    ]
}
