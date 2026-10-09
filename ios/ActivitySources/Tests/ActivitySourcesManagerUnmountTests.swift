import Foundation
import XCTest
import FjuulCore
import SwiftyMocky

@testable import FjuulActivitySources

/// Interleavings of `unmount()` with refreshes and in-flight mounts.
final class ActivitySourcesManagerUnmountTests: XCTestCase {
    var apiClientMock: ActivitySourcesApiClientMock!

    let credentials = UserCredentials(
        token: "b530b31f-74ca-4814-9e24-1bd35d5d1b61",
        secret: "9b28de21-905b-4ff3-8e66-7859e776e143"
    )

    let persistor = InMemoryPersistor()

    let config = ActivitySourceConfigBuilder { builder in
        builder.healthKitConfig = HealthKitActivitySourceConfig(dataTypesToRead: [.heartRate, .stepCount, .workout, ])
    }

    let healthKitTrackerConnection = TrackerConnection(id: "c7d1e5a3-6b2f-4e8d-9a4c-1f3b7e2d5a90", tracker: "healthkit", createdAt: Date(), endedAt: nil)

    override func setUp() {
        super.setUp()
        apiClientMock = ActivitySourcesApiClientMock()
    }

    func testRefreshCurrentCompletingAfterUnmountDoesNotMountConnections() {
        // Given
        let refreshed = expectation(description: "Refresh completes")
        let unmounted = expectation(description: "Unmount completes")
        let trackerConnection = TrackerConnection(id: "3f2b8c1e-5d47-4a9e-b6f1-2c8d7e4a9b03", tracker: "polar", createdAt: Date(), endedAt: nil)
        var pendingResponse: ((Result<[TrackerConnection], Error>) -> Void)?

        Perform(apiClientMock, .getCurrentConnections(completion: .any, perform: { (completion) in
            pendingResponse = completion
        }))
        let sut = makeManager()

        // When
        sut.refreshCurrent { result in
            if case .success(let connections) = result {
                XCTAssertEqual(connections.map { $0.id }, [trackerConnection.id], "Should still return the fetched connections")
            } else {
                XCTFail("Refresh should succeed")
            }
            refreshed.fulfill()
        }
        sut.unmount { _ in unmounted.fulfill() }
        wait(for: [unmounted], timeout: 5)
        pendingResponse?(.success([trackerConnection]))

        // Then
        wait(for: [refreshed], timeout: 5)
        XCTAssert(sut.mountedActivitySourceConnections.isEmpty, "Should not mount connections fetched before unmount")
        XCTAssertNil(ActivitySourcesStateStore(userToken: "b530b31f-74ca-4814-9e24-1bd35d5d1b61", persistor: persistor).connections,
                     "Should not persist connections fetched before unmount")
    }

    func testRefreshCurrentStartedAfterUnmountMountsConnections() {
        // Given
        let unmounted = expectation(description: "Unmount completes")
        let refreshed = expectation(description: "Refresh completes")
        let trackerConnection = TrackerConnection(id: "9a6e4d2f-1b8c-4f3a-a7d5-6e0b2c9f8a14", tracker: "polar", createdAt: Date(), endedAt: nil)

        Perform(apiClientMock, .getCurrentConnections(completion: .any, perform: { (completion) in
            completion(.success([trackerConnection]))
        }))
        let sut = makeManager()

        // When
        sut.unmount { _ in unmounted.fulfill() }
        wait(for: [unmounted], timeout: 5)
        sut.refreshCurrent { _ in refreshed.fulfill() }

        // Then
        wait(for: [refreshed], timeout: 5)
        XCTAssertEqual(sut.mountedActivitySourceConnections.map { $0.id }, [trackerConnection.id])
    }

    func testUnmountWaitsForInFlightMountAndUnmountsIt() {
        // Given
        let healthKitMock = MountableHealthKitActivitySourceMock()
        Given(healthKitMock, .trackerValue(getter: TrackerValue.HEALTHKIT))
        let mountStarted = expectation(description: "Mount starts")
        let refreshed = expectation(description: "Refresh completes")
        let unmounted = expectation(description: "Unmount completes")
        var pendingMount: ((Result<Void, Error>) -> Void)?
        var mountCompleted = false
        var unmountedBeforeMountCompleted = false

        Perform(healthKitMock, .mount(apiClient: .any, config: .any, healthKitManagerBuilder: .any, completion: .any, perform: { (_, _, _, completion) in
            pendingMount = completion
            mountStarted.fulfill()
        }))
        Perform(healthKitMock, .unmount(completion: .any, perform: { (completion) in
            unmountedBeforeMountCompleted = !mountCompleted
            completion(.success(()))
        }))
        Perform(apiClientMock, .getCurrentConnections(completion: .any, perform: { (completion) in
            completion(.success([self.healthKitTrackerConnection]))
        }))
        let sut = makeManager(connectingTo: healthKitMock)

        // When
        sut.refreshCurrent { _ in refreshed.fulfill() }
        wait(for: [mountStarted], timeout: 5)
        sut.unmount { result in
            if case .failure(let err) = result {
                XCTFail("Unmount should succeed: \(err)")
            }
            unmounted.fulfill()
        }
        mountCompleted = true
        pendingMount?(.success(()))

        // Then
        wait(for: [refreshed, unmounted], timeout: 5)
        XCTAssertFalse(unmountedBeforeMountCompleted, "Should unmount only after the in-flight mount completed")
        Verify(healthKitMock, 1, .unmount(completion: .any))
        XCTAssert(sut.mountedActivitySourceConnections.isEmpty)
    }

    func testRefreshCurrentStartedDuringUnmountMountsAfterIt() {
        // Given
        let healthKitMock = MountableHealthKitActivitySourceMock()
        Given(healthKitMock, .trackerValue(getter: TrackerValue.HEALTHKIT))
        let initiallyRefreshed = expectation(description: "Initial refresh completes")
        let unmountStarted = expectation(description: "Unmount starts")
        let unmounted = expectation(description: "Unmount completes")
        let refreshed = expectation(description: "Refresh completes")
        var pendingUnmount: ((Result<Void, Error>) -> Void)?

        Perform(healthKitMock, .mount(apiClient: .any, config: .any, healthKitManagerBuilder: .any, completion: .any, perform: { (_, _, _, completion) in
            completion(.success(()))
        }))
        Perform(healthKitMock, .unmount(completion: .any, perform: { (completion) in
            pendingUnmount = completion
            unmountStarted.fulfill()
        }))
        Perform(apiClientMock, .getCurrentConnections(completion: .any, perform: { (completion) in
            completion(.success([self.healthKitTrackerConnection]))
        }))
        let sut = makeManager(connectingTo: healthKitMock)
        sut.refreshCurrent { _ in initiallyRefreshed.fulfill() }
        wait(for: [initiallyRefreshed], timeout: 5)

        // When
        sut.unmount { _ in unmounted.fulfill() }
        wait(for: [unmountStarted], timeout: 5)
        sut.refreshCurrent { _ in refreshed.fulfill() }
        pendingUnmount?(.success(()))

        // Then
        wait(for: [unmounted, refreshed], timeout: 5)
        XCTAssertEqual(sut.mountedActivitySourceConnections.map { $0.id }, [healthKitTrackerConnection.id],
                       "A refresh started after unmount() should mount its connections once the unmount finished")
        Verify(healthKitMock, 2, .mount(apiClient: .any, config: .any, healthKitManagerBuilder: .any, completion: .any))
    }

    func testRefreshCurrentStartedBeforeDisconnectDoesNotRemountDisconnectedSource() {
        // Given
        let healthKitMock = MountableHealthKitActivitySourceMock()
        Given(healthKitMock, .trackerValue(getter: TrackerValue.HEALTHKIT))
        let initiallyRefreshed = expectation(description: "Initial refresh completes")
        let disconnected = expectation(description: "Disconnect completes")
        let refreshed = expectation(description: "Stale refresh completes")
        var getConnectionsCalls = 0
        var pendingResponse: ((Result<[TrackerConnection], Error>) -> Void)?

        Perform(healthKitMock, .mount(apiClient: .any, config: .any, healthKitManagerBuilder: .any, completion: .any, perform: { (_, _, _, completion) in
            completion(.success(()))
        }))
        Perform(healthKitMock, .unmount(completion: .any, perform: { (completion) in
            completion(.success(()))
        }))
        Perform(apiClientMock, .getCurrentConnections(completion: .any, perform: { (completion) in
            getConnectionsCalls += 1
            if getConnectionsCalls == 1 {
                completion(.success([self.healthKitTrackerConnection]))
            } else {
                pendingResponse = completion
            }
        }))
        Perform(apiClientMock, .disconnect(activitySourceConnection: .any, completion: .any, perform: { (_, completion) in
            completion(.success(()))
        }))
        let sut = makeManager(connectingTo: healthKitMock)
        sut.refreshCurrent { _ in initiallyRefreshed.fulfill() }
        wait(for: [initiallyRefreshed], timeout: 5)

        // When: the server answered this refresh before handling the disconnect
        sut.refreshCurrent { _ in refreshed.fulfill() }
        sut.disconnect(activitySourceConnection: sut.mountedActivitySourceConnections[0]) { result in
            if case .failure(let err) = result {
                XCTFail("Disconnect should succeed: \(err)")
            }
            disconnected.fulfill()
        }
        wait(for: [disconnected], timeout: 5)
        pendingResponse?(.success([healthKitTrackerConnection]))

        // Then
        wait(for: [refreshed], timeout: 5)
        XCTAssert(sut.mountedActivitySourceConnections.isEmpty, "Should not mount the disconnected source again")
        Verify(healthKitMock, 1, .mount(apiClient: .any, config: .any, healthKitManagerBuilder: .any, completion: .any))
        XCTAssertEqual(persistedConnectionIds(), [], "A manager created later should not restore the disconnected source")
    }

    func testOlderRefreshResponseArrivingLastDoesNotOverwriteNewerState() {
        // Given
        let olderTrackerConnection = TrackerConnection(id: "1b6f3d8a-9c2e-4a7f-b5d1-8e4c0a2f6b93", tracker: "polar", createdAt: Date(), endedAt: nil)
        let newerTrackerConnection = TrackerConnection(id: "8d4a2c6e-3f1b-4d9a-a7e5-2b9f6c1d0e47", tracker: "garmin", createdAt: Date(), endedAt: nil)
        let olderRefreshed = expectation(description: "Older refresh completes")
        let newerRefreshed = expectation(description: "Newer refresh completes")
        var getConnectionsCalls = 0
        var pendingOlderResponse: ((Result<[TrackerConnection], Error>) -> Void)?

        Perform(apiClientMock, .getCurrentConnections(completion: .any, perform: { (completion) in
            getConnectionsCalls += 1
            if getConnectionsCalls == 1 {
                pendingOlderResponse = completion
            } else {
                completion(.success([newerTrackerConnection]))
            }
        }))
        let sut = makeManager()

        // When
        sut.refreshCurrent { _ in olderRefreshed.fulfill() }
        sut.refreshCurrent { _ in newerRefreshed.fulfill() }
        wait(for: [newerRefreshed], timeout: 5)
        pendingOlderResponse?(.success([olderTrackerConnection]))

        // Then
        wait(for: [olderRefreshed], timeout: 5)
        XCTAssertEqual(sut.mountedActivitySourceConnections.map { $0.id }, [newerTrackerConnection.id])
        XCTAssertEqual(persistedConnectionIds(), [newerTrackerConnection.id])
    }

    func testRefreshCurrentUnmountsObsoleteSourcesBeforeMountingNewOnes() {
        // Given
        let healthKitMock = MountableHealthKitActivitySourceMock()
        Given(healthKitMock, .trackerValue(getter: TrackerValue.HEALTHKIT))
        let otherSourceMock = MountableHealthKitActivitySourceMock()
        Given(otherSourceMock, .trackerValue(getter: TrackerValue.POLAR))
        let otherTrackerConnection = TrackerConnection(id: "e2a9c4f1-7b3d-4c6e-8f5a-0d1b9e3c7a26", tracker: "polar", createdAt: Date(), endedAt: nil)
        let initiallyRefreshed = expectation(description: "Initial refresh completes")
        let refreshed = expectation(description: "Refresh completes")
        var events: [String] = []
        var getConnectionsCalls = 0

        Perform(healthKitMock, .mount(apiClient: .any, config: .any, healthKitManagerBuilder: .any, completion: .any, perform: { (_, _, _, completion) in
            completion(.success(()))
        }))
        Perform(healthKitMock, .unmount(completion: .any, perform: { (completion) in
            events.append("unmount started")
            DispatchQueue.global().asyncAfter(deadline: .now() + 0.1) {
                events.append("unmount finished")
                completion(.success(()))
            }
        }))
        Perform(otherSourceMock, .mount(apiClient: .any, config: .any, healthKitManagerBuilder: .any, completion: .any, perform: { (_, _, _, completion) in
            events.append("mount started")
            completion(.success(()))
        }))
        Perform(apiClientMock, .getCurrentConnections(completion: .any, perform: { (completion) in
            getConnectionsCalls += 1
            completion(.success(getConnectionsCalls == 1 ? [self.healthKitTrackerConnection] : [otherTrackerConnection]))
        }))
        let sut = makeManager { $0.tracker == "healthkit" ? healthKitMock : otherSourceMock }
        sut.refreshCurrent { _ in initiallyRefreshed.fulfill() }
        wait(for: [initiallyRefreshed], timeout: 5)

        // When
        sut.refreshCurrent { _ in refreshed.fulfill() }

        // Then
        wait(for: [refreshed], timeout: 5)
        XCTAssertEqual(events, ["unmount started", "unmount finished", "mount started"])
        XCTAssertEqual(sut.mountedActivitySourceConnections.map { $0.id }, [otherTrackerConnection.id])
        XCTAssertEqual(persistedConnectionIds(), [otherTrackerConnection.id])
    }

    /// With `activitySource`, every connection uses it, so mounting and unmounting can be controlled.
    private func makeManager(connectingTo activitySource: ActivitySource? = nil) -> ActivitySourcesManager {
        guard let activitySource = activitySource else {
            let client = ApiClient(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: persistor)
            return ActivitySourcesManager(userToken: client.userToken, persistor: persistor, apiClient: apiClientMock, config: config)
        }
        return makeManager { _ in activitySource }
    }

    private func makeManager(activitySourceFor: @escaping (TrackerConnection) -> ActivitySource) -> ActivitySourcesManager {
        let client = ApiClient(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: persistor)
        return ActivitySourcesManager(userToken: client.userToken, persistor: persistor, apiClient: apiClientMock, config: config,
                                      connectionFactory: { ActivitySourceConnection(trackerConnection: $0, activitySource: activitySourceFor($0)) })
    }

    private func persistedConnectionIds() -> [String]? {
        return ActivitySourcesStateStore(userToken: "b530b31f-74ca-4814-9e24-1bd35d5d1b61", persistor: persistor).connections?.map { $0.id }
    }
}
