import XCTest
@testable import FjuulCore

final class UserCredentialsTests: XCTestCase {

    func testAuthString() {
        let credentials = UserCredentials(
            token: "b530b31f-74ca-4814-9e24-1bd35d5d1b61",
            secret: "9b28de21-905b-4ff3-8e66-7859e776e143"
        )
        XCTAssertEqual(
            credentials.completeAuthString(),
            "Bearer YjUzMGIzMWYtNzRjYS00ODE0LTllMjQtMWJkMzVkNWQxYjYxOjliMjhkZTIxLTkwNWItNGZmMy04ZTY2LTc4NTllNzc2ZTE0Mw=="
        )
    }

}
