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
            \"gender\":\"female\",\"height\":170.2689,\"weight\":60.6481,
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
        let profile = PartialUserProfile { partial in
            partial[\.birthDate] = DateFormatters.yyyyMMddLocale.date(from: "1989-11-03")
            partial[\.gender] = Gender.other
            // NOTE: uncomment the lines below to check the deserialization of decimals initialized in a regular way.
//            partial[\.height] = 170.2689
//            partial[\.weight] = 60.6481
            partial[\.height] = Decimal(string: "170.2689")!
            partial[\.weight] = Decimal(string: "60.6481")!
        }
        let createStub = stub(condition: isHost("apibase") && isPath("/sdk/users/v1")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            do {
                let profileData: [String: Any] = try JSONSerialization.jsonObject(with: request.ohhttpStubs_httpBody!) as! [String: Any]
                XCTAssertEqual(profileData["birthDate"] as? String, "1989-11-03")
                XCTAssertEqual(profileData["gender"] as? String, "other")
                XCTAssertEqual(String(describing: profileData["height"]!), "170.2689")
                XCTAssertEqual(String(describing: profileData["weight"]!), "60.6481")
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
        let profile = PartialUserProfile { partial in
            partial[\.birthDate] = DateFormatters.yyyyMMddLocale.date(from: "1989-11-03")
            partial[\.gender] = Gender.other
            partial[\.height] = Decimal(string: "170.2")
            partial[\.weight] = Decimal(string: "60.6")
            partial[\.timezone] = TimeZone(identifier: "Europe/Paris")
            partial[\.locale] = "fi"
        }
        let createStub = stub(condition: isHost("apibase") && isPath("/sdk/users/v1")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            do {
                let profileData: [String: Any] = try JSONSerialization.jsonObject(with: request.ohhttpStubs_httpBody!) as! [String: Any]
                XCTAssertEqual(profileData["birthDate"] as? String, "1989-11-03")
                XCTAssertEqual(profileData["gender"] as? String, "other")
                XCTAssertEqual(String(describing: profileData["height"]!), "170.2")
                XCTAssertEqual(String(describing: profileData["weight"]!), "60.6")
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

    // swiftlint:disable function_body_length
    func testCreateUserWithValidationError() {
        let e = expectation(description: "Alamofire")
        let profile = PartialUserProfile { partial in
            partial[\.birthDate] = DateFormatters.yyyyMMddLocale.date(from: "1989-11-03")
            partial[\.gender] = Gender.other
            partial[\.height] = -170
            partial[\.weight] = -67.4
            partial[\.timezone] = TimeZone(identifier: "Europe/Paris")
            partial[\.locale] = "fi"
        }
        let createStub = stub(condition: isHost("apibase") && isPath("/sdk/users/v1")) { request in
            XCTAssertEqual(request.httpMethod, "POST")
            let json = """
            {
                \"message\": \"Bad Request: Validation error\",
                \"errors\": [
                    {\"property\":\"weight\",\"constraints\": {\"isPositive\": \"weight must be a positive number\"}, \"value\":-67.4},
                    {\"property\":\"height\",\"constraints\": {\"isPositive\": \"height must be a positive number\"}, \"value\":-170},
                    {\"property\":\"gender\",\"constraints\": {\"isIn\": \"gender must be one of the following values: male, female, other\"}, \"value\":\"blah\"}
                ]
            }
            """
            let stubData = json.data(using: String.Encoding.utf8)
            return HTTPStubsResponse(data: stubData!, statusCode: 400, headers: nil)
        }
        ApiClient.createUser(baseUrl: "https://apibase", apiKey: "", profile: profile) { result in
            switch result {
            case .success:
                XCTFail("Should be failed request")
            case .failure(let fjuulError):
                XCTAssertEqual(fjuulError.localizedDescription, "Bad Request: Validation error")

                switch fjuulError {
                case FjuulError.userFailure(let userError):
                    switch userError {
                    case .validation(let error):
                        XCTAssertEqual(error.errors.count, 3)

                        let firstError = error.errors[0]
                        XCTAssertEqual(firstError.property, "weight")
                        XCTAssertEqual(firstError.value, ValidationErrorValue.double(-67.4))
                        XCTAssertEqual(firstError.constraints.count, 1)
                        XCTAssertEqual(firstError.constraints["isPositive"], "weight must be a positive number")

                        let secondError = error.errors[1]
                        XCTAssertEqual(secondError.property, "height")
                        XCTAssertEqual(secondError.value, ValidationErrorValue.int(-170))
                        XCTAssertEqual(secondError.constraints.count, 1)
                        XCTAssertEqual(secondError.constraints["isPositive"], "height must be a positive number")

                        let thirdError = error.errors[2]
                        XCTAssertEqual(thirdError.property, "gender")
                        XCTAssertEqual(thirdError.value, ValidationErrorValue.string("blah"))
                        XCTAssertEqual(thirdError.constraints.count, 1)
                        XCTAssertEqual(thirdError.constraints["isIn"], "gender must be one of the following values: male, female, other")

                        e.fulfill()
                    default:
                        XCTFail("Wrong error type")
                    }
                default:
                    XCTFail("Wrong error type")
                }
            }

            HTTPStubs.removeStub(createStub)
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
                XCTAssertEqual(profile.height, Decimal(string: "170.2689"))
                XCTAssertEqual(profile.locale, "en")
                XCTAssertEqual(profile.timezone, TimeZone(identifier: "Europe/Berlin"))
                XCTAssertEqual(profile.token, "e15480c7-ec04-4436-be1e-39cbef384967")
                XCTAssertEqual(profile.weight, Decimal(string: "60.6481"))
            }
            e.fulfill()
        }
        waitForExpectations(timeout: 5.0, handler: nil)
    }

    func testUpdateProfile() {
        let e = expectation(description: "Alamofire")
        let client = ApiClient(baseUrl: "https://apibase", apiKey: "", credentials: credentials, persistor: InMemoryPersistor())
        let update = PartialUserProfile { profile in
            profile[\.weight] = 85
        }
        let updateStub = stub(condition: isHost("apibase") && pathMatches("^/sdk/users/v1/*")) { request in
            XCTAssertEqual(request.httpMethod, "PUT")
            do {
                let bodyData: [String: Any] = try JSONSerialization.jsonObject(with: request.ohhttpStubs_httpBody!) as! [String: Any]
                XCTAssertEqual(bodyData["weight"] as? Decimal, update[\.weight])
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
