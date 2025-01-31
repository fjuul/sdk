import Foundation
import XCTest
import FjuulCore
import SwiftyMocky

@testable import FjuulActivitySources

final class ActivitySourceConnectionTests: XCTestCase {
    var sut: ActivitySourceConnection!
    var apiClientMock: ActivitySourcesApiClientMock!
    var activitySourceHKMock: MountableHealthKitActivitySourceMock!

    let persistor = InMemoryPersistor()
    let config = ActivitySourceConfigBuilder { builder in
        builder.healthKitConfig = HealthKitActivitySourceConfig(dataTypesToRead: [.heartRate, .activeEnergyBurned, .workout, ])
    }

    let trackerConnection = TrackerConnection(id: "healthkit", tracker: "healthkit", createdAt: Date(), endedAt: nil)

    override func setUp() {
        super.setUp()

        activitySourceHKMock = MountableHealthKitActivitySourceMock()

        Given(activitySourceHKMock, .trackerValue(getter: TrackerValue.HEALTHKIT))

        apiClientMock = ActivitySourcesApiClientMock()

        sut = ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: activitySourceHKMock)

        Matcher.default.register(ActivitySourceConfigBuilder.self) { (lhs, rhs) -> Bool in
            return lhs.healthKitConfig.typesToRead == rhs.healthKitConfig.typesToRead
        }

        Matcher.default.register(Persistor.self) { (_, _) -> Bool in
            return true
        }

        Matcher.default.register(ActivitySourcesApiClient.self) { (_, _) -> Bool in
            return true
        }
    }

    func testInit() {
        // Then
        XCTAssertEqual(sut.id, trackerConnection.id)
        XCTAssertEqual(sut.tracker, TrackerValue(value: trackerConnection.tracker))
        XCTAssertEqual(sut.createdAt, trackerConnection.createdAt)
        XCTAssertEqual(sut.endedAt, trackerConnection.endedAt)
        XCTAssertEqual(sut.activitySource.trackerValue, HealthKitActivitySource.shared.trackerValue)
    }

    func testMountSucces() {
        // Given
        let promise = expectation(description: "Success mount")

        // When
        Perform(activitySourceHKMock, .mount(apiClient: .value(apiClientMock),
                                             config: .value(self.config), healthKitManagerBuilder: .any, completion: .any, perform: { (_, _, _, completion) in

            completion(.success(()))
        }))

        sut.mount(apiClient: apiClientMock, config: self.config, persistor: self.persistor) { result in

            switch result {
            case .success:
                promise.fulfill()
            case .failure(let err):
                XCTFail("Error: \(err.localizedDescription)")
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testMountFailure() {
        // Given
        let promise = expectation(description: "Failure mount")

        // When
        Perform(activitySourceHKMock, .mount(apiClient: .value(apiClientMock), config: .value(self.config),
                                             healthKitManagerBuilder: .any, completion: .any, perform: { (_, _, _, completion) in

            completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitNotAvailableOnDevice)))
        }))

        sut.mount(apiClient: apiClientMock, config: self.config, persistor: self.persistor) { result in

            switch result {
            case .success:
                XCTFail("Error: should not mount activitySource")
            case .failure:
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testUnmountSucces() {
        // Given
        let promise = expectation(description: "Success unmount")

        // When
        Perform(activitySourceHKMock, .unmount(completion: .any, perform: { (completion) in
            completion(.success(()))
        }))

        sut.unmount { result in
            switch result {
            case .success:
                promise.fulfill()
            case .failure(let err):
                XCTFail("Error: \(err.localizedDescription)")
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testUnmountFailure() {
        // Given
        let promise = expectation(description: "Failure unmount")

        // When
        Perform(activitySourceHKMock, .unmount(completion: .any, perform: { (completion) in
            completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitNotAvailableOnDevice)))
        }))

        sut.unmount { result in
            switch result {
            case .success:
                XCTFail("Error: should not unmount activitySource")
            case .failure:
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }
}
