import Foundation
import FjuulCore

public enum Gender: String, Codable {
    case male = "male"
    case female = "female"
    case other = "other"
}

public struct UserProfile: Codable {

    public let token: String
    public let birthDate: Date
    public let gender: Gender
    public let height: Int
    public let weight: Int
    public let timezone: String
    public let locale: String?

}

extension UserProfile: PartiallyEncodable {

    static public func key(for keyPath: PartialKeyPath<UserProfile>) -> String? {
        switch keyPath {
        case \UserProfile.birthDate : return "birthDate"
        case \UserProfile.gender: return "gender"
        case \UserProfile.height: return "height"
        case \UserProfile.weight: return "weight"
        case \UserProfile.timezone: return "timezone"
        case \UserProfile.locale: return "locale"
        default: return nil
        }
    }

    static public func jsonEncodableValue(for key: PartialKeyPath<UserProfile>, in value: Partial<UserProfile>) -> Any {
        switch key {
        case \UserProfile.birthDate: return DateFormatters.yyyyMMddLocale.string(from: value[\.birthDate]!)
        case \UserProfile.gender: return value[\.gender]!.rawValue
        default: return value[key]!
        }
    }

}
