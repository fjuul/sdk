import Foundation
import Alamofire

public struct SigningKey: CustomStringConvertible, Codable, Equatable {

    let id: String
    let secret: String
    let expiresAt: Date

    public var description: String {
        return "SigningKey(id: \(id), secret: ***, expiresAt: \(expiresAt)"
    }

}

extension SigningKey: AuthenticationCredential {

    public var requiresRefresh: Bool { Date(timeIntervalSinceNow: 60 * 5) > expiresAt }

}

extension Optional: AuthenticationCredential where Wrapped == SigningKey {

    public var requiresRefresh: Bool {
        guard let value = self else {
            return true
        }
        return value.requiresRefresh
    }

}
