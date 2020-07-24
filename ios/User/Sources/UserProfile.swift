import Foundation

public enum Gender: String, Codable {
    case male = "male"
    case female = "female"
    case other = "other"
}

public struct UserProfile: Codable {

    public let birthDate: Date
    public let gender: Gender
    public let height: Int
    public let weight: Int
    public let timezone: String
    //public let locale: String

}
