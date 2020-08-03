//swiftlint:disable force_cast
import Foundation

import XCTest
import OHHTTPStubs
import OHHTTPStubsSwift
import FjuulCore
@testable import FjuulUser

final class UserApiTests: XCTestCase {

    let signingKeyResponse = """
        {
            \"id\":\"d8ad4ea1-fff3-43d0-b4d4-7d007b3ee9ad\",
            \"secret\":\"bbLXRlZ0tN1uriURxNaaajwWPsTfVrvY408vFanPQDE=\",
            \"expiresAt\":\"2030-01-01T00:00:00.000Z\"
        }
    """

    let profileResponse = """
        {
            \"token\":\"e15480c7-ec04-4436-be1e-39cbef384967\",
            \"gender\":\"female\",\"height\":175,\"weight\":60,
            \"timezone\":\"Europe/Berlin\",\"locale\":\"en\",
            \"birthDate\":\"1990-12-04\"
        }
    """

    let credentials = UserCredentials(
        token: "e15480c7-ec04-4436-be1e-39cbef384967",
        secret: "9b28de21-905b-4ff3-8e66-7859e776e143"
    )

    override func setUp() {
        super.setUp()
        stub(condition: isHost("apibase") && isPath("/sdk/signing/v1/issue-key/user")) { _ in
            let stubData = self.signingKeyResponse.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 200, headers: nil)
        }
    }

    override func tearDown() {
        HTTPStubs.removeAllStubs()
        super.tearDown()
    }

    func testCreateUserWithDefaults() {
        let e = expectation(description: "Alamofire")
        let profile = PartialUserProfile([
            \UserProfile.birthDate: DateFormatters.yyyyMMddLocale.date(from: "1989-11-03"),
            \UserProfile.gender: Gender.other,
            \UserProfile.height: 170,
            \UserProfile.weight: 60,
        ])
        let createStub = stub(condition: isHost("apibase") && isPath("/sdk/users/v1")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            do {
                let profileData: [String: Any] = try JSONSerialization.jsonObject(with: request.ohhttpStubs_httpBody!) as! [String: Any]
                XCTAssertEqual(profileData["birthDate"] as? String, "1989-11-03")
                XCTAssertEqual(profileData["gender"] as? String, "other")
                XCTAssertEqual(profileData["height"] as? Int, profile[\.height])
                XCTAssertEqual(profileData["weight"] as? Int, profile[\.weight])
                XCTAssertEqual(profileData["timezone"] as? String, TimeZone.current.identifier)
                XCTAssertEqual(profileData["locale"] as? String, Bundle.main.preferredLocalizations.first)
                XCTAssertEqual(profileData.count, 6)
            } catch {
                XCTFail("body deserialization failed")
            }
            return HTTPStubsResponse(data: Data(), statusCode: 200, headers: nil)
        }
        ApiClient.createUser(baseUrl: "https://apibase", apiKey: "", profile: profile) { _ in
            HTTPStubs.removeStub(createStub)
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testCreateUser() {
        let e = expectation(description: "Alamofire")
        let profile = PartialUserProfile([
            \UserProfile.birthDate: DateFormatters.yyyyMMddLocale.date(from: "1989-11-03"),
            \UserProfile.gender: Gender.other,
            \UserProfile.height: 170,
            \UserProfile.weight: 60,
            \UserProfile.timezone: TimeZone(identifier: "Europe/Paris"),
            \UserProfile.locale: "fi",
        ])
        let createStub = stub(condition: isHost("apibase") && isPath("/sdk/users/v1")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            do {
                let profileData: [String: Any] = try JSONSerialization.jsonObject(with: request.ohhttpStubs_httpBody!) as! [String: Any]
                XCTAssertEqual(profileData["birthDate"] as? String, "1989-11-03")
                XCTAssertEqual(profileData["gender"] as? String, "other")
                XCTAssertEqual(profileData["height"] as? Int, profile[\.height])
                XCTAssertEqual(profileData["weight"] as? Int, profile[\.weight])
                XCTAssertEqual(profileData["timezone"] as? String, "Europe/Paris")
                XCTAssertEqual(profileData["locale"] as? String, "fi")
                XCTAssertEqual(profileData.count, 6)
            } catch {
                XCTFail("body deserialization failed")
            }
            return HTTPStubsResponse(data: Data(), statusCode: 200, headers: nil)
        }
        ApiClient.createUser(baseUrl: "https://apibase", apiKey: "", profile: profile) { _ in
            HTTPStubs.removeStub(createStub)
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testGetProfile() {
        let e = expectation(description: "Alamofire")
        let client = ApiClient(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: InMemoryPersistor())
        let getStub = stub(condition: isHost("apibase") && pathMatches("^/sdk/users/v1/*")) { request in
            XCTAssertEqual(request.httpMethod, "GET")
            let profileData = self.profileResponse.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: profileData!, statusCode: 200, headers: nil)
        }
        client.user.getProfile { result in
            HTTPStubs.removeStub(getStub)
            switch result {
            case .failure: XCTFail("response deserialization failed")
            case .success(let profile):
                XCTAssertEqual(profile.birthDate, DateFormatters.yyyyMMddLocale.date(from: "1990-12-04"))
                XCTAssertEqual(profile.gender, Gender.female)
                XCTAssertEqual(profile.height, 175)
                XCTAssertEqual(profile.locale, "en")
                XCTAssertEqual(profile.timezone, TimeZone(identifier: "Europe/Berlin"))
                XCTAssertEqual(profile.token, "e15480c7-ec04-4436-be1e-39cbef384967")
                XCTAssertEqual(profile.weight, 60)
            }
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testUpdateProfile() {
        let e = expectation(description: "Alamofire")
        let client = ApiClient(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: InMemoryPersistor())
        let update = PartialUserProfile([
            \UserProfile.weight: 85,
        ])
        let updateStub = stub(condition: isHost("apibase") && pathMatches("^/sdk/users/v1/*")) { request in
            XCTAssertEqual(request.httpMethod, "PUT")
            do {
                let bodyData: [String: Any] = try JSONSerialization.jsonObject(with: request.ohhttpStubs_httpBody!) as! [String: Any]
                print(bodyData)
                XCTAssertEqual(bodyData["weight"] as? Int, update[\.weight])
                XCTAssertEqual(bodyData.count, 1)
            } catch {
                XCTFail("body deserialization failed")
            }
            return HTTPStubsResponse(data: Data(), statusCode: 200, headers: nil)
        }
        client.user.updateProfile(update) { _ in
            HTTPStubs.removeStub(updateStub)
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

}
