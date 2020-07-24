import Foundation
import FjuulCore

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

    public init(birthDate: Date, gender: Gender, height: Int, weight: Int, timezone: TimeZone = TimeZone.current) {
        self.birthDate = birthDate
        self.gender = gender
        self.height = height
        self.weight = weight
        self.timezone = timezone.identifier
    }

}

extension UserProfile: PartiallyEncodable {

    static public func key(for keyPath: PartialKeyPath<UserProfile>) -> String? {
        switch keyPath {
        case \UserProfile.birthDate : return "birthDate"
        case \UserProfile.gender: return "gender"
        case \UserProfile.height: return "height"
        case \UserProfile.weight: return "weight"
        case \UserProfile.timezone: return "timezone"
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
