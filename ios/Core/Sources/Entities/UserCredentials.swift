import Foundation

struct UserCredentials: CustomStringConvertible {

    let token: String
    let secret: String

    func completeAuthString() -> String {
        return "Bearer \(encodedBase64())"
    }

    func encodedBase64() -> String {
        let foo = "\(token):\(secret)"
        return Data(foo.utf8).base64EncodedString()
    }

    var description: String {
        return "UserCredentials(token: \(token), secret: ***)"
    }

}
