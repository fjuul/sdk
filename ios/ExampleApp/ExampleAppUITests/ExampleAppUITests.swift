import XCTest

class ExampleAppUITests: XCTestCase {

    func testCallbackFailureFeedback() throws {
        let app = XCUIApplication()
        app.launch()
        let fixtures = [
            ("&errorCode=oauth_cancelled", "Connection Cancelled"),
            ("&errorCode=google_health_account_not_linked", "Google Health Account Required"),
            ("&errorCode=future_code", "Connection Failed"),
            ("&errorCode=", "Connection Failed"),
            ("&unrelated=value", "Connection Failed"),
            ("", "Connection Failed"),
        ]
        for (suffix, title) in fixtures {
            try openCallback(app: app, query: "service=googlehealth&success=false\(suffix)")
            let alert = app.alerts[title]
            XCTAssertTrue(alert.waitForExistence(timeout: 5))
            if title == "Google Health Account Required" {
                XCTAssertTrue(alert.buttons["Account Setup"].exists)
                XCTAssertTrue(alert.staticTexts["Create a Google Health profile or migrate your Fitbit account, then return and retry the connection."].exists)
            } else {
                XCTAssertFalse(alert.buttons["Account Setup"].exists)
            }
            alert.buttons["OK"].tap()
        }
    }

    func testSuccessIgnoresErrorCode() throws {
        let app = XCUIApplication()
        app.launch()
        try openCallback(app: app, query: "service=googlehealth&success=true&errorCode=google_health_account_not_linked")
        XCTAssertFalse(app.alerts.firstMatch.waitForExistence(timeout: 2))
    }

    func testColdStartCallbackFailure() throws {
        let app = XCUIApplication()
        app.terminate()
        try openCallback(app: app, query: "service=googlehealth&success=false&errorCode=google_health_account_not_linked")
        XCTAssertTrue(app.alerts["Google Health Account Required"].waitForExistence(timeout: 5))
    }

    private func openCallback(app: XCUIApplication, query: String) throws {
        guard #available(iOS 16.4, *) else {
            throw XCTSkip("Opening callback URLs requires iOS 16.4 or later.")
        }
        app.open(URL(string: "fjuulsdk-exampleapp://external_connect?\(query)")!)
    }

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation -
        // required for your tests before they run. The setUp method is a good place to do this.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    func testLaunchPerformance() throws {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, *) {
            // This measures how long it takes to launch your application.
            measure(metrics: [XCTOSSignpostMetric.applicationLaunch]) {
                XCUIApplication().launch()
            }
        }
    }

}
