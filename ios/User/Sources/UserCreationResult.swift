import Foundation

public struct UserCreationResult: Decodable {

    public let user: UserProfile
    public let secret: String

}
