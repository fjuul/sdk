import Foundation

public struct UserCreationResult: Codable {

    public let user: UserProfile
    public let secret: String

}
