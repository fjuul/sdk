import Foundation

import XCTest
import OHHTTPStubs
import OHHTTPStubsSwift
import FjuulCore
@testable import FjuulActivitySources

// swiftlint:disable type_body_length
final class ActivitySourcesApiTests: XCTestCase {
    var sut: ActivitySourcesApi!
    var apiClient: ApiClient!

    let signingKeyResponse = """
        {
            \"id\":\"d8ad4ea1-fff3-43d0-b4d4-7d007b3ee9ad\",
            \"secret\":\"bbLXRlZ0tN1uriURxNaaajwWPsTfVrvY408vFanPQDE=\",
            \"expiresAt\":\"2030-01-01T00:00:00.000Z\"
        }
    """

    let credentials = UserCredentials(
        token: "b530b31f-74ca-4814-9e24-1bd35d5d1b61",
        secret: "9b28de21-905b-4ff3-8e66-7859e776e143"
    )

    override func setUp() {
        super.setUp()

        stub(condition: isHost("apibase") && isPath("/sdk/signing/v1/issue-key/user")) { _ in
            let stubData = self.signingKeyResponse.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }

        apiClient = ApiClient(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: InMemoryPersistor())

        sut = ActivitySourcesApi(apiClient: apiClient)
    }

    override func tearDown() {
        sut = nil
        HTTPStubs.removeAllStubs()
        super.tearDown()
    }

    func testConnectHealthKitTracker() {
        let e = expectation(description: "Request on connect HealthKit activity source")

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(apiClient.userToken)/connections/\(TrackerValue.HEALTHKIT.value)")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            let json = """
                {
                    \"id\": \"0ca60422-3626-4b50-aa70-43c91d8da731\", \"tracker\": \"healthkit\", \"createdAt\": \"2020-12-07T15:23:57.397Z\", \"endedAt\": null
                }
            """
            let stubData = json.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 201, headers: nil)
        }

        sut.connect(trackerValue: TrackerValue.HEALTHKIT) { result in
            switch result {
            case .success(let connectionResult):
                switch connectionResult {
                case .connected(let trackerConnection):
                    XCTAssertEqual(trackerConnection.tracker, "healthkit")
                    XCTAssertEqual(trackerConnection.id, "0ca60422-3626-4b50-aa70-43c91d8da731")
                    e.fulfill()
                case .externalAuthenticationFlowRequired:
                    XCTFail("Error")
                }
            case .failure:
                XCTFail("Network level failure")
            }
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testConnectExternalTracker() {
        let e = expectation(description: "Request on connect external tracker")

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(apiClient.userToken)/connections/\(TrackerValue.POLAR.value)")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            let json = """
                {
                    \"url\": \"https://flow.polar.com/oauth2/authorization?response_type=code&client_id=71fyfQ  0  0\"
                }
            """
            let stubData = json.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }

        sut.connect(trackerValue: TrackerValue.POLAR) { result in
            switch result {
            case .success(let connectionResult):
                switch connectionResult {
                case .connected:
                    XCTFail("Connection result should not be connected, must ask auth flow")
                case .externalAuthenticationFlowRequired:
                    e.fulfill()
                }
            case .failure:
                XCTFail("Network level failure")
            }
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testConnectWithAlreadyConnectedTracker() {
        let e = expectation(description: "Request on connect external tracker")

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(apiClient.userToken)/connections/\(TrackerValue.POLAR.value)")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            let stubData = "{ \"message\": \"Response status code was unacceptable: 409.\" }".data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 409, headers: nil)
        }

        sut.connect(trackerValue: TrackerValue.POLAR) { result in
            switch result {
            case .success(let connectionResult):
                switch connectionResult {
                case .connected:
                    XCTFail("Connection result should not be connected, must ask auth flow")
                case .externalAuthenticationFlowRequired:
                    XCTFail("Connection result should not ask auth flow")
                }
            case .failure(let err):
                switch err as? FjuulError {
                case .activitySourceConnectionFailure(let reason):
                    switch reason {
                    case .sourceAlreadyConnected(let message):
                        XCTAssertEqual(message, "Response status code was unacceptable: 409.")
                    default:
                        XCTFail("Wrong error type")
                    }
                default:
                    XCTFail("Wrong error type")
                }
                XCTAssertEqual(err.localizedDescription,
                   FjuulError.activitySourceConnectionFailure(reason: .sourceAlreadyConnected(message: "Response status code was unacceptable: 409.")).localizedDescription)
                e.fulfill()
            }
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testDisconnectTracker() {
        let e = expectation(description: "Request on disconnect tracker")

        let trackerConnection = TrackerConnection(id: "0ca60422-3626-4b50-aa70-43c91d8da731", tracker: "healthkit", createdAt: Date(), endedAt: nil)
        let activitySourceConnection = ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: HealthKitActivitySource.shared)

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(apiClient.userToken)/connections/\(activitySourceConnection.id)")) { request in
            XCTAssertEqual(request.httpMethod, "DELETE")
            return HTTPStubsResponse(data: Data(), statusCode: 204, headers: nil)
        }

        sut.disconnect(activitySourceConnection: activitySourceConnection) { result in
            switch result {
            case .success:
                e.fulfill()
            case .failure(let err):
                XCTFail("Network level failure: \(err)")
            }
        }

        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testGetCurrentConnections() {
        let e = expectation(description: "Request on get current connections")

        let hkTracker = TrackerConnection(id: "0ca60422-3626-4b50-aa70-43c91d8da731", tracker: "healthkit", createdAt: Date(), endedAt: nil)
        let polarTracker = TrackerConnection(id: "43c91d8da731-3626-4b50-aa70-0ca60422", tracker: "polar", createdAt: Date(), endedAt: nil)

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(apiClient.userToken)/connections")) { request in
            XCTAssertEqual(request.httpMethod, "GET")
            let json = """
                [
                    {
                        \"id\": \"\(hkTracker.id)\", \"tracker\": \"\(hkTracker.tracker)\", \"createdAt\": \"2020-11-07T15:23:57.397Z\", \"endedAt\": null
                    },
                    {
                        \"id\": \"\(polarTracker.id)\", \"tracker\": \"\(polarTracker.tracker)\", \"createdAt\": \"2020-11-07T15:23:57.397Z\", \"endedAt\": null
                    }
                ]
            """
            let stubData = json.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }

        sut.getCurrentConnections { result in
            switch result {
            case .success(let connections):
                XCTAssertEqual(connections.first!.id, hkTracker.id)
                XCTAssertEqual(connections.first!.tracker, hkTracker.tracker)

                XCTAssertEqual(connections.last!.id, polarTracker.id)
                XCTAssertEqual(connections.last!.tracker, polarTracker.tracker)

                e.fulfill()
            case .failure(let err):
                XCTFail("Network level failure: \(err)")
            }
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testInitActivitySourcesManager() {
        // Given
        let config = ActivitySourceConfigBuilder { builder in
            builder.healthKitConfig = HealthKitActivitySourceConfig(dataTypesToRead: [.stepCount, .workout, ])
        }
        // Check that it's nil before initialize
        XCTAssertNil(apiClient.activitySourcesManager)

        // When
        apiClient.initActivitySourcesManager(config: config)

        // Then
        XCTAssertNotNil(apiClient.activitySourcesManager)
    }

    func testInitActivitySourcesManagerWithCompletion() {
        // Given
        let config = ActivitySourceConfigBuilder { builder in
            builder.healthKitConfig = HealthKitActivitySourceConfig(dataTypesToRead: [.stepCount, .workout])
        }
        // Check that it's nil before initialize
        XCTAssertNil(apiClient.activitySourcesManager)

        // When
        apiClient.initActivitySourcesManager(config: config) { result in
            switch result {
            case .success:
                XCTAssert(true)
            case .failure(let err):
                XCTFail("Error on init ActivitySourcesManager \(err)")
            }
        }

        // Then
        XCTAssertNotNil(apiClient.activitySourcesManager)
    }

    func testSendHealthKitBatchData() {
        let e = expectation(description: "Request on send batch data")

        let createStub = stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(apiClient.userToken)/healthkit")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            return HTTPStubsResponse(data: Data(), statusCode: 200, headers: nil)
        }

        let entries = [AggregatedDataPoint(value: 3.141592, start: Date())]
        let batches = [BatchDataPoint(sourceBundleIdentifiers: ["com.apple.health.ADBA62D3-FDA1-413C-AA68-874E1D1A9DF1"], entries: entries)]
        let batchData = HKBatchData(caloriesData: batches)

        sut.sendHealthKitBatchData(data: batchData) { result in
            switch result {
            case .success:
                e.fulfill()
            case .failure:
                XCTFail("Network level failure")
            }
            HTTPStubs.removeStub(createStub)
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testSendHealthKitUserProfileData() {
        let e = expectation(description: "Request on send user profile data")

        let createStub = stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(apiClient.userToken)/healthkit/profile")) { request in
            XCTAssertEqual(request.httpMethod, "PUT")
            return HTTPStubsResponse(data: Data(), statusCode: 200, headers: nil)
        }

        let userProfileData = HKUserProfileData(height: 167.5, weight: 67)

        sut.sendHealthKitUserProfileData(data: userProfileData) { result in
            switch result {
            case .success:
                e.fulfill()
            case .failure:
                XCTFail("Network level failure")
            }

            HTTPStubs.removeStub(createStub)
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testSendHealthKitUserProfileDataWithValidationError() {
        let e = expectation(description: "Request on send user profile data")

        let createStub = stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(apiClient.userToken)/healthkit/profile")) { request in
            XCTAssertEqual(request.httpMethod, "PUT")
            let json = """
            {
                \"message\": \"Bad Request: Validation error\",
                \"errors\": [
                    {\"property\":\"weight\",\"constraints\": {\"isPositive\": \"weight must be a positive number\"}, \"value\":0},
                    {\"property\":\"height\",\"constraints\": {\"isPositive\": \"height must be a positive number\"}, \"value\":0},
                ]
            }
            """
            let stubData = json.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 400, headers: nil)
        }

        let userProfileData = HKUserProfileData(height: 0, weight: 0)

        sut.sendHealthKitUserProfileData(data: userProfileData) { result in
            switch result {
            case .success:
                XCTFail("Should be failed request")
            case .failure(let fjuulError):
                XCTAssertEqual(fjuulError.localizedDescription, "Bad Request: Validation error")
                e.fulfill()
            }

            HTTPStubs.removeStub(createStub)
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }
}
