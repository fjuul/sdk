import Foundation

import XCTest
//import OHHTTPStubs
//import OHHTTPStubsSwift
import FjuulCore
@testable import FjuulUser

final class UserApiTests: XCTestCase {

    let credentials = UserCredentials(
        token: "b530b31f-74ca-4814-9e24-1bd35d5d1b61",
        secret: "9b28de21-905b-4ff3-8e66-7859e776e143"
    )

    override func setUp() {
        super.setUp()
    }

    override func tearDown() {
        //HTTPStubs.removeAllStubs()
        super.tearDown()
    }

}
