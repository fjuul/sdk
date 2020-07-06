import Foundation

struct SigningKey: CustomStringConvertible {

    // swiftlint:disable:next identifier_name
    let id: String
    let secret: String
    let expiresAt: Date

    var description: String {
        return "SigningKey(id: \(id), secret: ***, expiresAt: \(expiresAt)"
    }

    var requiresRefresh: Bool { Date(timeIntervalSinceNow: 60 * 5) > expiresAt }

}
