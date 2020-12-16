import Foundation
import XCTest
import FjuulCore
import OHHTTPStubs
import OHHTTPStubsSwift
import HealthKit

@testable import FjuulActivitySources

extension ActivitySourceManager {
    static func reset() {
        shared.apiClient = nil
        shared.mountedActivitySourceConnections = []
        shared.config = nil
    }
}


// TODO: May be remove it
class StubHKHealthStore: HKHealthStore {
    override public func requestAuthorization(toShare typesToShare: Set<HKSampleType>?, read typesToRead: Set<HKObjectType>?, completion: @escaping (Bool, Error?) -> Void) {
        completion(true, nil)
    }
}

class ApiClientStub: ApiClient {
//    override public func requestAuthorization(toShare typesToShare: Set<HKSampleType>?, read typesToRead: Set<HKObjectType>?, completion: @escaping (Bool, Error?) -> Void) {
//        completion(true, nil)
//    }
}

final class ActivitySourceManagerTests: XCTestCase {
    var sut: ActivitySourceManager!

    override func setUp() {
        super.setUp()
        sut = ActivitySourceManager.shared
    }

    override func tearDown() {
        sut = nil
        ActivitySourceManager.reset()
//        HTTPStubs.removeAllStubs()
        super.tearDown()
    }

    let credentials = UserCredentials(
        token: "b530b31f-74ca-4814-9e24-1bd35d5d1b61",
        secret: "9b28de21-905b-4ff3-8e66-7859e776e143"
    )

    let signingKeyResponse = """
        {
            \"id\":\"d8ad4ea1-fff3-43d0-b4d4-7d007b3ee9ad\",
            \"secret\":\"bbLXRlZ0tN1uriURxNaaajwWPsTfVrvY408vFanPQDE=\",
            \"expiresAt\":\"2030-01-01T00:00:00.000Z\"
        }
    """

    let persistor = InMemoryPersistor()
    
    let config = ActivitySourceConfigBuilder { builder in
        builder.healthKitConfig = ActivitySourceHKConfig(dataTypesToRead: [.heartRate, .activeEnergyBurned, .distanceCycling,
                                                                           .distanceWalkingRunning, .stepCount, .workoutType, ])
    }

    func testUnitializedState() {
        XCTAssert(sut.apiClient == nil, "Wrong configuration")
        XCTAssert(sut.mountedActivitySourceConnections.isEmpty, "Wrong configuration")
        XCTAssert(sut.config == nil, "Wrong configuration")
    }

    func testInitializeWithEmptyStoredActyvityConnections() {
        // Given
        let client = ApiClientStub(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: persistor)
//        let config = ActivitySourceConfigBuilder { builder in
//            builder.healthKitConfig = ActivitySourceHKConfig(dataTypesToRead: [.heartRate, .activeEnergyBurned, .distanceCycling, .distanceWalkingRunning, .stepCount, .workoutType])
//        }

        // When
        sut.initialize(apiClient: client, config: config)

        // Then
        XCTAssert(sut.apiClient != nil, "Api client should not be empty")
        XCTAssert(sut.mountedActivitySourceConnections.isEmpty, "Should not mount any ActivitySourceConnections")
        XCTAssert(sut.config != nil, "config should not be empty")
    }

    func testInitializeWithExistsStoredActyvityConnections() {
        // Given
        let client = ApiClientStub(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: persistor)
//        let config = ActivitySourceConfigBuilder { builder in
//            builder.healthKitConfig = ActivitySourceHKConfig(dataTypesToRead: [.heartRate, .activeEnergyBurned, .distanceCycling, .distanceWalkingRunning, .stepCount, .workoutType])
//        }

        let trackerConnections = [
            TrackerConnection(id: "polar", tracker: "polar", createdAt: Date(), endedAt: nil)
        ]

        let connectionsLocalStore = ActivitySourceStore(userToken: client.userToken, persistor: persistor)
        connectionsLocalStore.connections = trackerConnections

        // When
        sut.initialize(apiClient: client, config: config)

        // Then
        XCTAssert(sut.apiClient != nil, "Api client should not be empty")
        XCTAssert(!sut.mountedActivitySourceConnections.isEmpty, "Should mount ActivitySourceConnections stored in local store")
        XCTAssert(sut.mountedActivitySourceConnections.count == trackerConnections.count, "Incorrect mounted ActivitySourceConnections")
        XCTAssert(sut.mountedActivitySourceConnections.contains { element in element.tracker == .polar }, "Incorrect mounted ActivitySourceConnections")
        XCTAssert(sut.config != nil, "config should not be empty")
    }

    func testConnectHealthKitActivitySource() {
        // Given
        HealthKitManager.healthStore = StubHKHealthStore()

        let client = ApiClientStub(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: persistor)
//        let config = ActivitySourceConfigBuilder { builder in
//            builder.healthKitConfig = ActivitySourceHKConfig(dataTypesToRead: [.heartRate, .activeEnergyBurned, .distanceCycling,
//                                                                               .distanceWalkingRunning, .stepCount, .workoutType, ])
//        }

        stub(condition: isHost("apibase") && isPath("/sdk/signing/v1/issue-key/user")) { _ in
            let stubData = self.signingKeyResponse.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(client.userToken)/connections")) { _ in
            let stubData = "[]".data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }

        let createStub = stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(client.userToken)/connections/\(ActivitySourcesItem.healthkit.rawValue)")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            let json = """
                {
                    \"id\": \"0ca60422-3626-4b50-aa70-43c91d8da731\", \"tracker\": \"healthkit\", \"createdAt\": \"2020-12-07T15:23:57.397Z\", \"endedAt\": null
                }
            """
            let stubData = json.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 201, headers: nil)
        }

        let promise = expectation(description: "Success connect HealthKit activity source")

        sut.initialize(apiClient: client, config: config)

        // When
        sut.connect(activitySource: ActivitySourceHK.shared) { result in
            switch result {
            case .success(let connectionResult):
                switch connectionResult {
                case .connected(let trackerConnection):
                    XCTAssertEqual(trackerConnection.id, "0ca60422-3626-4b50-aa70-43c91d8da731")
                    XCTAssertEqual(trackerConnection.tracker, ActivitySourcesItem.healthkit.rawValue)
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
        let client = ApiClientStub(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: persistor)
//        let config = ActivitySourceConfigBuilder { builder in
//            builder.healthKitConfig = ActivitySourceHKConfig(dataTypesToRead: [.heartRate, .activeEnergyBurned, .distanceCycling,
//                                                                               .distanceWalkingRunning, .stepCount, .workoutType, ])
//        }

        stub(condition: isHost("apibase") && isPath("/sdk/signing/v1/issue-key/user")) { _ in
            let stubData = self.signingKeyResponse.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(client.userToken)/connections")) { _ in
            let stubData = "[]".data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }

        let createStub = stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(client.userToken)/connections/\(ActivitySourcesItem.polar.rawValue)")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            let json = """
                {
                    \"url\": \"https://flow.polar.com/oauth2/authorization?response_type=code&client_id=71fyfQ  0  0\"
                }
            """
            let stubData = json.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }

        sut.initialize(apiClient: client, config: config)

        let promise = expectation(description: "Success get external authentication URL")

        // When
        sut.connect(activitySource: ActivitySourcePolar.shared) { result in
            switch result {
            case .success(let connectionResult):
                switch connectionResult {
                case .connected:
                    XCTFail("Error: connection should not be connected")
                case .externalAuthenticationFlowRequired(let authenticationUrl):
                    if authenticationUrl.contains("https://flow.polar.com/oauth2/authorization?") {
                        promise.fulfill()
                    }
                }
            case .failure(let err):
                XCTFail("Error: \(err.localizedDescription)")
            }
        }
        wait(for: [promise], timeout: 5)
        HTTPStubs.removeStub(createStub)
    }

    func testDisconnectActivitySource() {
        // Given
        let polarTrackerID = "0ca60422-3626-4b50-aa70-43c91d8da731"
        let client = ApiClientStub(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: persistor)
        stub(condition: isHost("apibase") && isPath("/sdk/signing/v1/issue-key/user")) { _ in
            let stubData = self.signingKeyResponse.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(client.userToken)/connections")) { _ in
            let json = """
                [
                    { \"id\": \"\(polarTrackerID)\", \"tracker\": \"polar\", \"createdAt\": \"2020-12-07T15:23:57.397Z\", \"endedAt\": null }
                ]
            """
            let stubData = json.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }
        
        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(client.userToken)/connections/\(polarTrackerID)")) { request in
            XCTAssertEqual(request.httpMethod, "DELETE")
            let stubData = self.signingKeyResponse.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }

        // Local state
        let trackerConnections = [
            TrackerConnection(id: polarTrackerID, tracker: "polar", createdAt: Date(), endedAt: nil)
        ]

        let connectionsLocalStore = ActivitySourceStore(userToken: client.userToken, persistor: persistor)
        connectionsLocalStore.connections = trackerConnections

        let promise = expectation(description: "Success disconnected request")

        // When
        sut.initialize(apiClient: client, config: config)
        let activitySourceConnection = sut.mountedActivitySourceConnections.last
        
        XCTAssertEqual(sut.mountedActivitySourceConnections.count, 1)
        
        // Then
        sut.disconnect(activitySourceConnection: activitySourceConnection!) { result in
            switch result {
            case .success:
                promise.fulfill()
            case .failure(let err):
                XCTFail("Error: \(err.localizedDescription)")
            }
        }
        wait(for: [promise], timeout: 5)
        
        XCTAssertEqual(sut.mountedActivitySourceConnections.count, 0)
    }
}
