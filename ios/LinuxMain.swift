import XCTest

import CoreTests
import AnalyticsTests

var tests = [XCTestCaseEntry]()
tests += CoreTests.allTests()
tests += AnalyticsTests.allTests()
XCTMain(tests)
