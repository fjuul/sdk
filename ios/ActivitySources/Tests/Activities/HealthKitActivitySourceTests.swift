import Foundation
import XCTest
import FjuulCore
import SwiftyMocky

@testable import FjuulActivitySources

extension HealthKitActivitySource {
    func reset() {
        self.apiClient = nil
    }
}

// swiftlint:disable file_length
// swiftlint:disable type_body_length
final class ActivitySourceHKTests: XCTestCase {
    var sut: HealthKitActivitySource!
    var activitySourcesApiClientMock: ActivitySourcesApiClientMock!
    var healthKitManagerBuilderMock: HealthKitManagerBuildingMock!
    var healthKitManagingMock: HealthKitManagingMock!

    let config = ActivitySourceConfigBuilder { builder in
        builder.healthKitConfig = HealthKitActivitySourceConfig(dataTypesToRead: [.heartRate, .activeEnergyBurned, .workout, ])
    }
    let persistor = InMemoryPersistor()

    override func setUp() {
        super.setUp()

        sut = HealthKitActivitySource.shared

        activitySourcesApiClientMock = ActivitySourcesApiClientMock()
        healthKitManagerBuilderMock = HealthKitManagerBuildingMock()
        healthKitManagingMock = HealthKitManagingMock()
    }

    override func tearDown() {
        super.tearDown()

        HealthKitActivitySource.shared.reset()
    }

    func testInit() {
        //Then
        XCTAssertNil(sut.apiClient)
        XCTAssertEqual(sut.trackerValue, TrackerValue.HEALTHKIT)
    }

    func testSuccessMount() {
        // Given
        let promise = expectation(description: "Success mount")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        Perform(healthKitManagingMock, .mount(completion: .any, perform: { (completion) in
            completion(.success(()))
        }))

        //When
        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success:
                promise.fulfill()
            case .failure(let err):
                XCTFail("Error: \(err.localizedDescription)")
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testFailureMount() {
        // Given
        let promise = expectation(description: "Failure mount")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        Perform(healthKitManagingMock, .mount(completion: .any, perform: { (completion) in
            completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitNotAvailableOnDevice)))
        }))

        //When
        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success:
                XCTFail("Error: should not unmount")
            case .failure:
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testSuccessUnmount() {
        // Given
        let promise = expectation(description: "Success unmount")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        Perform(healthKitManagingMock, .mount(completion: .any, perform: { (completion) in
            completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitNotAvailableOnDevice)))
        }))

        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success:
                XCTFail("Error: should mount activitySource")
            case .failure:
                XCTAssert(true)
            }
        }

        Perform(healthKitManagingMock, .disableAllBackgroundDelivery(completion: .any, perform: { (completion) in
            completion(.success(()))
        }))

        //When
        sut.unmount { result in
            switch result {
            case .success:
                promise.fulfill()
            case .failure:
                XCTFail("Error: should not fails")
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testFailureUnmountWhenFaileDisableAllBackgroundDelivery() {
        // Given
        let promise = expectation(description: "Failure unmount")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        Perform(healthKitManagingMock, .mount(completion: .any, perform: { (completion) in
            completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitNotAvailableOnDevice)))
        }))

        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success:
                XCTFail("Error: should mount activitySource")
            case .failure:
                XCTAssert(true)
            }
        }

        Perform(healthKitManagingMock, .disableAllBackgroundDelivery(completion: .any, perform: { (completion) in
            completion(.failure(FjuulError.activitySourceFailure(reason: .activitySourceNotMounted)))
        }))

        //When
        sut.unmount { result in
            switch result {
            case .success:
                XCTFail("Error: should not unmount")
            case .failure:
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testFailureUnmountWhenNotYetMounted() {
        // Given
        let promise = expectation(description: "Failure unmount")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        Perform(healthKitManagingMock, .mount(completion: .any, perform: { (completion) in
            completion(.failure(FjuulError.activitySourceFailure(reason: .healthkitNotAvailableOnDevice)))
        }))

        Perform(healthKitManagingMock, .disableAllBackgroundDelivery(completion: .any, perform: { (completion) in
            completion(.success(()))
        }))

        //When
        sut.unmount { result in
            switch result {
            case .success:
                XCTFail("Error: should not unmount")
            case .failure:
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testSuccessSyncIntradayMetrics() {
        // Given
        let promise = expectation(description: "Success sync intradayMetrics")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success:
                XCTFail("Error: should mount activitySource")
            case .failure:
                XCTAssert(true)
            }
        }

        let startDate = Calendar.current.startOfDay(for: Date())
        let endDate = Date()

        Perform(healthKitManagingMock, .sync(startDate: .value(startDate), endDate: .value(endDate), configTypes: .value(HealthKitConfigType.intradayTypes),
                                             completion: .any, perform: { (_, _, _, completion) in
            completion(.success(()))
        }))

        //When
        sut.syncIntradayMetrics(startDate: startDate, endDate: endDate) { result in
            switch result {
            case .success:
                promise.fulfill()
            case .failure:
                XCTFail("Error: should not fails")
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testFailureSyncIntradayMetricsWhenActivitySourceNotMounted() {
        // Given
        let promise = expectation(description: "Failure sync intradayMetrics")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success:
                XCTFail("Error: should mount activitySource")
            case .failure:
                XCTAssert(true)
            }
        }

        let startDate = Calendar.current.startOfDay(for: Date())
        let endDate = Date()

        Perform(healthKitManagingMock, .sync(startDate: .value(startDate), endDate: .value(endDate), configTypes: .value(HealthKitConfigType.intradayTypes),
                                             completion: .any, perform: { (_, _, _, completion) in
            completion(.failure(FjuulError.activitySourceFailure(reason: .activitySourceNotMounted)))
        }))

        //When
        sut.syncIntradayMetrics(startDate: startDate, endDate: endDate) { result in
            switch result {
            case .success:
                XCTFail("Error: should not successfully sync")
            case .failure:
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testFailureSyncIntradayMetricsWhenWrongType() {
        // Given
        let promise = expectation(description: "Failure sync intradayMetrics")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success:
                XCTFail("Error: should mount activitySource")
            case .failure:
                XCTAssert(true)
            }
        }

        let startDate = Calendar.current.startOfDay(for: Date())
        let endDate = Date()

        //When
        sut.syncIntradayMetrics(startDate: startDate, endDate: endDate, configTypes: [.workout]) { result in
            switch result {
            case .success:
                XCTFail("Error: should not successfully sync")
            case .failure(let error):
                XCTAssertEqual(error.localizedDescription, FjuulError.activitySourceFailure(reason: .illegalHealthKitConfigType).localizedDescription)
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testSuccessSyncWorkouts() {
        // Given
        let promise = expectation(description: "Success sync workouts")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success:
                XCTFail("Error: should mount activitySource")
            case .failure:
                XCTAssert(true)
            }
        }

        let startDate = Calendar.current.startOfDay(for: Date())
        let endDate = Date()

        Perform(healthKitManagingMock, .sync(startDate: .value(startDate), endDate: .value(endDate), configTypes: .value([.workout]),
                                             completion: .any, perform: { (_, _, _, completion) in
            completion(.success(()))
        }))

        //When
        sut.syncWorkouts(startDate: startDate, endDate: endDate) { result in
            switch result {
            case .success:
                promise.fulfill()
            case .failure:
                XCTFail("Error: should not fails")
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testFailureSyncWorkoutsWhenActivitySourceNotMounted() {
        // Given
        let promise = expectation(description: "Failure sync workouts")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success:
                XCTFail("Error: should mount activitySource")
            case .failure:
                XCTAssert(true)
            }
        }

        let startDate = Calendar.current.startOfDay(for: Date())
        let endDate = Date()

        Perform(healthKitManagingMock, .sync(startDate: .value(startDate), endDate: .value(endDate), configTypes: .value([.workout]),
                                             completion: .any, perform: { (_, _, _, completion) in
            completion(.failure(FjuulError.activitySourceFailure(reason: .activitySourceNotMounted)))
        }))

        //When
        sut.syncWorkouts(startDate: startDate, endDate: endDate) { result in
            switch result {
            case .success:
                XCTFail("Error: should not successfully sync")
            case .failure:
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testSuccessSyncProfile() {
        // Given
        let promise = expectation(description: "Success sync profile")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success:
                XCTFail("Error: should mount activitySource")
            case .failure:
                XCTAssert(true)
            }
        }

        Perform(healthKitManagingMock, .sync(startDate: .value(nil), endDate: .value(nil), configTypes: .value(HealthKitConfigType.userProfileTypes),
                                             completion: .any, perform: { (_, _, _, completion) in
            completion(.success(()))
        }))

        //When
        sut.syncProfile { result in
            switch result {
            case .success:
                promise.fulfill()
            case .failure:
                XCTFail("Error: should not fails")
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testFailureSyncProfileWhenActivitySourceNotMounted() {
        // Given
        let promise = expectation(description: "Failure sync profile")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success:
                XCTFail("Error: should mount activitySource")
            case .failure:
                XCTAssert(true)
            }
        }

        Perform(healthKitManagingMock, .sync(startDate: .value(nil), endDate: .value(nil), configTypes: .value(HealthKitConfigType.userProfileTypes),
                                             completion: .any, perform: { (_, _, _, completion) in
            completion(.failure(FjuulError.activitySourceFailure(reason: .activitySourceNotMounted)))
        }))

        //When
        sut.syncProfile { result in
            switch result {
            case .success:
                XCTFail("Error: should not successfully sync")
            case .failure:
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testFailureSyncProfileWhenhWrongType() {
        // Given
        let promise = expectation(description: "Failure sync profile")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success:
                XCTFail("Error: should mount activitySource")
            case .failure:
                XCTAssert(true)
            }
        }

        //When
        sut.syncProfile(configTypes: [.workout]) { result in
            switch result {
            case .success:
                XCTFail("Error: should not successfully sync")
            case .failure(let error):
                XCTAssertEqual(error.localizedDescription, FjuulError.activitySourceFailure(reason: .illegalHealthKitConfigType).localizedDescription)
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }
}
