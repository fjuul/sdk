import Foundation
import XCTest
import FjuulCore

@testable import FjuulActivitySources

final class HealthKitQueryPredicateBuilderTests: XCTestCase {
    var healthKitActivitySourceConfig = HealthKitActivitySourceConfig(dataTypesToRead: [
        .heartRate, .activeEnergyBurned, .distanceCycling,
        .distanceWalkingRunning, .stepCount, .workout,
    ])

    override func setUp() {
        super.setUp()
    }

    override func tearDown() {
        super.tearDown()
    }

    func testDataСollectionStartAtWithDateRangeWhenStartDateLateThanDefault() {
        let startDate = Calendar.current.date(byAdding: .day, value: -7, to: Date())!
        let endDate = Date()

        let sut = HealthKitQueryPredicateBuilder(
            healthKitConfig: healthKitActivitySourceConfig,
            startDate: startDate,
            endDate: endDate
        )
        XCTAssertEqual(sut.dataСollectionStartAt(), startDate)
    }

    func testDataСollectionStartAtWithDateRangeWhenStartDateOlderThanDefault() {
        let startDate = Calendar.current.date(byAdding: .day, value: -31, to: Date())!
        let endDate = Date()

        let sut = HealthKitQueryPredicateBuilder(
            healthKitConfig: healthKitActivitySourceConfig,
            startDate: startDate,
            endDate: endDate
        )

        // limit is 30 days back
        XCTAssertEqual(sut.dataСollectionStartAt(),
                       Calendar.current.startOfDay(for: Calendar.current.date(byAdding: .day, value: -30, to: Date())!))
    }

    func testDataСollectionStartAtWithDateRangeWhenStartDateOlderThanSyncDataFrom() {
        let startDate = Calendar.current.date(byAdding: .day, value: -20, to: Date())!
        let endDate = Date()

        let syncDataFrom = Calendar.current.date(byAdding: .day, value: -15, to: Date())!

        // Usefull for setup TrackerConnection.createAt, for not sync data before Date of create connection
        healthKitActivitySourceConfig.syncDataFrom = syncDataFrom

        let sut = HealthKitQueryPredicateBuilder(
            healthKitConfig: healthKitActivitySourceConfig,
            startDate: startDate,
            endDate: endDate
        )

        XCTAssertEqual(sut.dataСollectionStartAt(), syncDataFrom)
    }

    func testDataСollectionEndAtWhenEndDateNotPresent() {
        let sut = HealthKitQueryPredicateBuilder(
            healthKitConfig: healthKitActivitySourceConfig
        )

        // Returns current date
        XCTAssertEqual(sut.dataСollectionEndAt().timeIntervalSince1970, Date().timeIntervalSince1970, accuracy: 1)
    }

    func testDataСollectionEndAtWhenEndDateLaterThanDataСollectionStartAt() {
        let startDate = Calendar.current.date(byAdding: .day, value: -20, to: Date())!
        let endDate = Date()

        let sut = HealthKitQueryPredicateBuilder(
            healthKitConfig: healthKitActivitySourceConfig,
            startDate: startDate,
            endDate: endDate
        )

        XCTAssertEqual(sut.dataСollectionEndAt(), endDate)
    }

    func testDataСollectionEndAtWhenEndDateOlderThanDataСollectionStartAt() {
        let startDate = Calendar.current.date(byAdding: .day, value: -20, to: Date())!
        let endDate = Calendar.current.date(byAdding: .day, value: -21, to: Date())!

        let sut = HealthKitQueryPredicateBuilder(
            healthKitConfig: healthKitActivitySourceConfig,
            startDate: startDate,
            endDate: endDate
        )

        // Use start date, for avoid case when endDate is earlier than startDate
        XCTAssertEqual(sut.dataСollectionEndAt(), startDate)
    }
}
