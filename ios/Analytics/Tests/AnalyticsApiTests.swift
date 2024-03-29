import Foundation

import XCTest
import OHHTTPStubs
import OHHTTPStubsSwift
import FjuulCore
@testable import FjuulAnalytics

final class AnalyticsApiTests: XCTestCase {

    let signingKeyResponse = """
        {
            \"id\":\"d8ad4ea1-fff3-43d0-b4d4-7d007b3ee9ad\",
            \"secret\":\"bbLXRlZ0tN1uriURxNaaajwWPsTfVrvY408vFanPQDE=\",
            \"expiresAt\":\"2030-01-01T00:00:00.000Z\"
        }
    """

    let singleDailyStatsResponse = """
        {
            \"date\":\"2020-06-10\",
            \"low\":{\"seconds\":180,\"metMinutes\":8.24},
            \"moderate\":{\"seconds\":1260,\"metMinutes\":89.8},
            \"high\":{\"seconds\":540,\"metMinutes\":63.23},
            \"bmr\":1755.64,\"activeKcal\":755.64,\"steps\":10000
        }
    """

    let aggregatedDailyStatsResponse = """
        {
            \"low\":{\"seconds\":80,\"metMinutes\":4.44},
            \"moderate\":{\"seconds\":160,\"metMinutes\":5.58},
            \"high\":{\"seconds\":110,\"metMinutes\":6.23},
            \"bmr\":705.64,\"activeKcal\":255.64,\"steps\":7400
        }
    """

    let credentials = UserCredentials(
        token: "b530b31f-74ca-4814-9e24-1bd35d5d1b61",
        secret: "9b28de21-905b-4ff3-8e66-7859e776e143"
    )

    override func setUp() {
        super.setUp()
        let multipleDailyStatsResponse = """
            [\(singleDailyStatsResponse),\(singleDailyStatsResponse),\(singleDailyStatsResponse)]
        """
        stub(condition: isHost("apibase") && pathMatches("^/sdk/analytics/v1/daily-stats/*") && pathEndsWith("aggregated")) { _ in
            let stubData = self.aggregatedDailyStatsResponse.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }
        stub(condition: isHost("apibase") && pathMatches("^/sdk/analytics/v1/daily-stats/*") && !pathEndsWith("aggregated")) { request in
            let isMultiRequest = request.url?.query?.contains("from") ?? false
            let response = isMultiRequest ? multipleDailyStatsResponse : self.singleDailyStatsResponse
            let stubData = response.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }
        stub(condition: isHost("apibase") && isPath("/sdk/signing/v1/issue-key/user")) { _ in
            let stubData = self.signingKeyResponse.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }
    }

    override func tearDown() {
        HTTPStubs.removeAllStubs()
        super.tearDown()
    }

    func testGetSingleDailyStats() {
        let e = expectation(description: "Alamofire")
        let client = ApiClient(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: InMemoryPersistor())
        client.analytics.dailyStats(date: Date()) { result in
            switch result {
            case .success(let dailyStats):
                XCTAssertEqual(dailyStats.moderate.seconds, 1260)
            case .failure:
                XCTFail("network level failure")
            }
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testGetMultipleDailyStats() {
        let e = expectation(description: "Alamofire")
        let client = ApiClient(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: InMemoryPersistor())
        client.analytics.dailyStats(from: Date.distantPast, to: Date.distantFuture) { result in
            switch result {
            case .success(let dailyStats):
                XCTAssertEqual(dailyStats.count, 3)
                XCTAssertEqual(dailyStats.first?.moderate.seconds, 1260)
                XCTAssertEqual(dailyStats.last?.moderate.seconds, 1260)
            case .failure:
                XCTFail("network level failure")
            }
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testGetAggregatedDailyStats() {
        let e = expectation(description: "Alamofire")
        let client = ApiClient(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: InMemoryPersistor())
        client.analytics.aggregatedDailyStats(from: Date.distantPast, to: Date.distantFuture, aggregation: AggregationType.sum) { result in
            switch result {
            case .success(let stat):
                XCTAssertEqual(stat.moderate.seconds, 160)
            case .failure:
                XCTFail("network level failure")
            }
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

}
