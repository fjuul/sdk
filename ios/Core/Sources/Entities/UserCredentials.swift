import Foundation

public struct UserCredentials: CustomStringConvertible, Codable, Sendable {

    let token: String
    let secret: String

    public init(token: String, secret: String) {
        self.token = token
        self.secret = secret
    }

    func completeAuthString() -> String {
        return "Bearer \(encodedBase64())"
    }

    func encodedBase64() -> String {
        let foo = "\(token):\(secret)"
        return Data(foo.utf8).base64EncodedString()
    }

    public var description: String {
        return "UserCredentials(token: \(token), secret: ***)"
    }

}
