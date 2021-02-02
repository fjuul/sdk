import Foundation
import XCTest
import FjuulCore
import SwiftyMocky

@testable import FjuulActivitySources

final class ActivitySourceManagerTests: XCTestCase {
    var sut: ActivitySourceManager!
    var apiClientMock: ActivitySourcesApiClientMock!

    let credentials = UserCredentials(
        token: "b530b31f-74ca-4814-9e24-1bd35d5d1b61",
        secret: "9b28de21-905b-4ff3-8e66-7859e776e143"
    )

    let persistor = InMemoryPersistor()

    let config = ActivitySourceConfigBuilder { builder in
        builder.healthKitConfig = ActivitySourceHKConfig(dataTypesToRead: [.heartRate, .activeEnergyBurned, .distanceCycling,
            .distanceWalkingRunning, .stepCount, .workoutType, ])
    }

    override func setUp() {
        super.setUp()

        let client = ApiClient(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: persistor)
        apiClientMock = ActivitySourcesApiClientMock()

        sut = ActivitySourceManager(userToken: client.userToken, persistor: persistor, apiClient: apiClientMock, config: config)
    }

    override func tearDown() {
        sut = nil
        super.tearDown()
    }

    func testInitializeWithEmptyStoredActyvityConnections() {
        // Then
        XCTAssert(sut.mountedActivitySourceConnections.isEmpty, "Should not mount ActivitySourceConnections")
        XCTAssert(sut.mountedActivitySourceConnections.count == 0, "Incorrect mounted ActivitySourceConnections")
    }

    func testInitializeWithExistsStoredActyvityConnections() {
        // Given
        let client = ApiClient(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: persistor)

        let trackerConnections = [
            TrackerConnection(id: "polar", tracker: "polar", createdAt: Date(), endedAt: nil),
            TrackerConnection(id: "unknow", tracker: "unknow", createdAt: Date(), endedAt: nil),
        ]

        let connectionsLocalStore = ActivitySourceStore(userToken: client.userToken, persistor: persistor)
        connectionsLocalStore.connections = trackerConnections

        // When
        let sut = ActivitySourceManager(userToken: client.userToken, persistor: persistor, apiClient: apiClientMock, config: config)

        // Then
        XCTAssertNotNil(sut.apiClient)
        XCTAssertNotNil(sut.config)
        XCTAssert(!sut.mountedActivitySourceConnections.isEmpty, "Should mount ActivitySourceConnections stored in local store")
        XCTAssert(sut.mountedActivitySourceConnections.count == trackerConnections.count)
        XCTAssert(sut.mountedActivitySourceConnections.first?.tracker == TrackerValue.POLAR)
        XCTAssert(sut.mountedActivitySourceConnections.last?.tracker == TrackerValue(value: "unknow"))

        let isInstanceOfActivitySourceUnknown = sut.mountedActivitySourceConnections.last?.activitySource is ActivitySourceUnknown
        if !isInstanceOfActivitySourceUnknown {
            XCTFail("Unknow tracker should be instance of ActivitySourceUnknown")
        }
    }

    func testConnectHealthKitActivitySource() {
        // Given
        let promise = expectation(description: "Success connect HealthKit activity source")

        let healthKitMock = MountableActivitySourceHKMock()
        Given(healthKitMock, .trackerValue(getter: TrackerValue.HEALTHKIT))

        Perform(apiClientMock, .connect(trackerValue: .value(TrackerValue.HEALTHKIT), completion: .any, perform: { (item, completion) in
            let trackerConnection = TrackerConnection(id: "0ca60422-3626-4b50-aa70-43c91d8da731", tracker: "healthkit", createdAt: Date(), endedAt: nil)
            completion(.success(.connected(trackerConnection: trackerConnection)))
        }))

        Perform(healthKitMock, .requestAccess(config: .any, completion: .any, perform: { (item, completion) in
           completion(.success(true))
        }))

        // When
        sut.connect(activitySource: healthKitMock) { result in
            switch result {
            case .success(let connectionResult):
                switch connectionResult {
                case .connected(let trackerConnection):
                    XCTAssertEqual(trackerConnection.id, "0ca60422-3626-4b50-aa70-43c91d8da731")
                    XCTAssertEqual(trackerConnection.tracker, TrackerValue.HEALTHKIT.value)
                    XCTAssertEqual(trackerConnection.endedAt, nil)

                    promise.fulfill()
                case .externalAuthenticationFlowRequired:
                    XCTFail("Error: connection should not require external authentication flow")
                }
            case .failure(let err):
                XCTFail("Error: \(err.localizedDescription)")
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testConnectExternalActivitySource() {
        // Given
        let promise = expectation(description: "Success get external authentication URL")

        let polarAuthUrl = "https://flow.polar.com/oauth2/authorization?response_type=code&client_id=71fyfQ"
        Perform(apiClientMock, .connect(trackerValue: .value(TrackerValue.POLAR), completion: .any, perform: { (item, completion) in

            completion(.success(.externalAuthenticationFlowRequired(authenticationUrl: polarAuthUrl)))
        }))

        // When
        sut.connect(activitySource: ActivitySourcePolar.shared) { result in
            switch result {
            case .success(let connectionResult):
                switch connectionResult {
                case .connected:
                    XCTFail("Error: external connection should not be connected without second step (OAuth flow)")
                case .externalAuthenticationFlowRequired(let authenticationUrl):
                    XCTAssertEqual(authenticationUrl, polarAuthUrl)
                    promise.fulfill()
                }
            case .failure(let err):
                XCTFail("Error: \(err.localizedDescription)")
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testConnectWithServerError() {
        // Given
        let promise = expectation(description: "Handle server response with error")

        Perform(apiClientMock, .connect(trackerValue: .value(TrackerValue.POLAR), completion: .any, perform: { (item, completion) in
            completion(.failure(FjuulError.activitySourceConnectionFailure(reason: .sourceAlreadyConnected)))
        }))

        // When
        sut.connect(activitySource: ActivitySourcePolar.shared) { result in
            switch result {
            case .success(let connectionResult):
                switch connectionResult {
                case .connected:
                    XCTFail("Error: connection should not be success on hendle server error")
                case .externalAuthenticationFlowRequired:
                    XCTFail("Error: connection should not be success on hendle server error")
                }
            case .failure:
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testDisconnectActivitySource() {
        // Given
        let promise = expectation(description: "Success disconnect activity source")

        let trackerConnection = TrackerConnection(id: "0ca60422", tracker: "polar", createdAt: Date(), endedAt: nil)
        let activitySourceConnection = ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: ActivitySourcePolar.shared)

        Perform(apiClientMock, .disconnect(activitySourceConnection: .value(activitySourceConnection), completion: .any, perform: { (item, completion) in

            completion(.success(()))
        }))

        // When
        sut.disconnect(activitySourceConnection: activitySourceConnection) { result in
            switch result {
            case .success:
                promise.fulfill()
            case .failure(let err):
                XCTFail("Error: \(err.localizedDescription)")
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testDisconnectActivitySourceWithServerError() {
        // Given
        let promise = expectation(description: "Handle server failure on disconnect activity source")

        let trackerConnection = TrackerConnection(id: "0ca60422", tracker: "polar", createdAt: Date(), endedAt: nil)
        let activitySourceConnection = ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: ActivitySourcePolar.shared)

        Perform(apiClientMock, .disconnect(activitySourceConnection: .value(activitySourceConnection), completion: .any, perform: { (item, completion) in

            completion(.failure(FjuulError.invalidConfig))
        }))

        // When
        sut.disconnect(activitySourceConnection: activitySourceConnection) { result in
            switch result {
            case .success:
                XCTFail("Result should not be success")
            case .failure:
                promise.fulfill()
            }
        }
        wait(for: [promise], timeout: 5)
    }

    func testRefreshCurrent() {
        // Given
        let promise = expectation(description: "Success disconnect activity source")

        let trackerConnection = TrackerConnection(id: "0ca60422", tracker: "polar", createdAt: Date(), endedAt: nil)

        Perform(apiClientMock, .getCurrentConnections(completion: .any, perform: { (completion) in
            completion(.success([trackerConnection]))
        }))

        // When
        sut.refreshCurrent { result in
            switch result {
            case .success(let connections):
                let expectedConnectionsList = [
                    ActivitySourceConnectionFactory.activitySourceConnection(trackerConnection: trackerConnection),
                ].compactMap { item in item }

                XCTAssert(connections == expectedConnectionsList, "Incorrect connections")
                promise.fulfill()
            case .failure(let err):
                XCTFail("Error: \(err.localizedDescription)")
            }
        }
        wait(for: [promise], timeout: 5)
    }
}
