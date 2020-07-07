import Foundation
import Alamofire

public class ApiClient {

    let sessionBuilder: HttpSessionBuilder
    public let signedSession: Session

    public init(baseUrl: String, apiKey: String, credentials: UserCredentials) {
        self.sessionBuilder = HttpSessionBuilder(baseUrl: baseUrl, apiKey: apiKey, credentials: credentials)
        self.signedSession = sessionBuilder.buildSignedSession()
    }

    public var baseUrl: String {
        get { return sessionBuilder.baseUrl }
    }

    public var userToken: String {
        get { return sessionBuilder.credentials.token }
    }

}
