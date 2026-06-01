import Foundation
import XCTest

@testable import FjuulActivitySources

final class DateUtilsTests: XCTestCase {
    private func makeUTCDate(_ value: String) -> Date {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.date(from: value)!
    }

    func testStartOfDaySetsTimeComponentsToMidnight() {
        let date = Date()
        let startOfDay = DateUtils.startOfDay(date: date)

        let components = Calendar.current.dateComponents([.hour, .minute, .second], from: startOfDay)
        XCTAssertEqual(components.hour, 0)
        XCTAssertEqual(components.minute, 0)
        XCTAssertEqual(components.second, 0)
    }

    func testEndOfHourSetsTimeComponentsToLastSecondOfHour() {
        let date = makeUTCDate("2023-07-12T23:35:42.000Z")
        let endOfHour = DateUtils.endOfHour(date: date)

        XCTAssertNotNil(endOfHour)

        let components = Calendar.current.dateComponents([.minute, .second], from: endOfHour!)
        XCTAssertEqual(components.minute, 59)
        XCTAssertEqual(components.second, 59)
    }

    func testDirtyUTCHoursWithinSingleHourReturnsSingleUTCHourStart() {
        let startDate = makeUTCDate("2023-07-12T23:35:00.000Z")
        let endDate = makeUTCDate("2023-07-12T23:47:00.000Z")

        let result = DateUtils.dirtyUTCHours(startDate: startDate, endDate: endDate)

        XCTAssertEqual(result.count, 1)
        XCTAssertTrue(result.contains(makeUTCDate("2023-07-12T23:00:00.000Z")))
    }

    func testDirtyUTCHoursCrossingMidnightReturnsTwoUTCHourStarts() {
        let startDate = makeUTCDate("2023-07-12T23:35:00.000Z")
        let endDate = makeUTCDate("2023-07-13T00:29:00.000Z")

        let result = DateUtils.dirtyUTCHours(startDate: startDate, endDate: endDate)

        XCTAssertEqual(result.count, 2)
        XCTAssertTrue(result.contains(makeUTCDate("2023-07-12T23:00:00.000Z")))
        XCTAssertTrue(result.contains(makeUTCDate("2023-07-13T00:00:00.000Z")))
    }
}
