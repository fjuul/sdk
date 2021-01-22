import Foundation

import XCTest
import OHHTTPStubs
import OHHTTPStubsSwift
import FjuulCore
@testable import FjuulActivitySources

final class ActivitySourcesApiTests: XCTestCase {
    var sut: ApiClient!

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

        sut = ApiClient(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: InMemoryPersistor())
    }

    override func tearDown() {
        sut = nil
        HTTPStubs.removeAllStubs()
        super.tearDown()
    }

    func testConnectHealthKitTracker() {
        let e = expectation(description: "Request on connect HealthKit activity source")

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(sut.userToken)/connections/\(ActivitySourcesItem.healthkit.rawValue)")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            let json = """
                {
                    \"id\": \"0ca60422-3626-4b50-aa70-43c91d8da731\", \"tracker\": \"healthkit\", \"createdAt\": \"2020-12-07T15:23:57.397Z\", \"endedAt\": null
                }
            """
            let stubData = json.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 201, headers: nil)
        }

        sut.activitySources.connect(activitySourceItem: ActivitySourcesItem.healthkit) { result in
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

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(sut.userToken)/connections/\(ActivitySourcesItem.polar.rawValue)")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            let json = """
                {
                    \"url\": \"https://flow.polar.com/oauth2/authorization?response_type=code&client_id=71fyfQ  0  0\"
                }
            """
            let stubData = json.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }

        sut.activitySources.connect(activitySourceItem: ActivitySourcesItem.polar) { result in
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

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(sut.userToken)/connections/\(ActivitySourcesItem.polar.rawValue)")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            let stubData = "".data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 409, headers: nil)
        }

        sut.activitySources.connect(activitySourceItem: ActivitySourcesItem.polar) { result in
            switch result {
            case .success(let connectionResult):
                switch connectionResult {
                case .connected:
                    XCTFail("Connection result should not be connected, must ask auth flow")
                case .externalAuthenticationFlowRequired:
                    XCTFail("Connection result should not ask auth flow")
                }
            case .failure(let err):
                XCTAssertEqual(err.localizedDescription, FjuulError.activitySourceConnectionFailure(reason: .sourceAlreadyConnected).localizedDescription)
                e.fulfill()
            }
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testDisconnectTracker() {
        let e = expectation(description: "Request on disconnect tracker")

        let trackerConnection = TrackerConnection(id: "0ca60422-3626-4b50-aa70-43c91d8da731", tracker: "healthkit", createdAt: Date(), endedAt: nil)
        let activitySourceConnection = ActivitySourceConnection(trackerConnection: trackerConnection, activitySource: ActivitySourceHK.shared)

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(sut.userToken)/connections/\(activitySourceConnection.id)")) { request in
            XCTAssertEqual(request.httpMethod, "DELETE")
            let stubData = "".data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 204, headers: nil)
        }

        sut.activitySources.disconnect(activitySourceConnection: activitySourceConnection) { result in
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

        stub(condition: isHost("apibase") && isPath("/sdk/activity-sources/v1/\(sut.userToken)/connections")) { request in
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

        sut.activitySources.getCurrentConnections { result in
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
}
