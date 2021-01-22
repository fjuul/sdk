import Foundation
import XCTest
import FjuulCore
import SwiftyMocky

@testable import FjuulActivitySources

extension ActivitySourceHK {
    func reset() {
        self.apiClient = nil
    }
}

final class ActivitySourceHKTests: XCTestCase {
    var sut: ActivitySourceHK!
    var activitySourcesApiClientMock: ActivitySourcesApiClientMock!
    var healthKitManagerBuilderMock: HealthKitManagerBuilderingMock!
    var healthKitManagingMock: HealthKitManagingMock!

    let config = ActivitySourceConfigBuilder { builder in
        builder.healthKitConfig = ActivitySourceHKConfig(dataTypesToRead: [.heartRate, .activeEnergyBurned, .workoutType, ])
    }
    let persistor = InMemoryPersistor()

    override func setUp() {
        super.setUp()

        sut = ActivitySourceHK.shared

        activitySourcesApiClientMock = ActivitySourcesApiClientMock()
        healthKitManagerBuilderMock = HealthKitManagerBuilderingMock()
        healthKitManagingMock = HealthKitManagingMock()
    }

    override func tearDown() {
        super.tearDown()

        ActivitySourceHK.shared.reset()
    }

    func testInit() {
        //Then
        XCTAssertNil(sut.apiClient)
        XCTAssertEqual(sut.tracker, ActivitySourcesItem.healthkit)
    }

    func testRequestAccess() {
        // Given
    }

    func testSuccessMount() {
        // Given
        let promise = expectation(description: "Success mount")

        Given(healthKitManagerBuilderMock, .create(dataHandler: .any, willReturn: healthKitManagingMock))

        Perform(healthKitManagingMock, .mount(completion: .any, perform: { (completion) in
            completion(.success(true))
        }))

        //When
        sut.mount(apiClient: activitySourcesApiClientMock, config: self.config, healthKitManagerBuilder: healthKitManagerBuilderMock) { result in
            switch result {
            case .success(let success):
                XCTAssert(success)
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
            completion(.success(true))
        }))

        //When
        sut.unmount { result in
            switch result {
            case .success(let success):
                XCTAssert(success)
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
            completion(.success(true))
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

    func testSuccessSync() {
        // Given
        let promise = expectation(description: "Success sync")

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

        Perform(healthKitManagingMock, .sync(completion: .any, perform: { (completion) in
            completion(.success(true))
        }))

        //When
        sut.sync { result in
            switch result {
            case .success(let success):
                XCTAssert(success)
                promise.fulfill()
            case .failure:
                XCTFail("Error: should not fails")
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testFailureSync() {
        // Given
        let promise = expectation(description: "Failure sync")

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

        Perform(healthKitManagingMock, .sync(completion: .any, perform: { (completion) in
            completion(.failure(FjuulError.activitySourceFailure(reason: .activitySourceNotMounted)))
        }))

        //When
        sut.sync { result in
            switch result {
            case .success:
                XCTFail("Error: should not successfully sync")
            case .failure:
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }
}
