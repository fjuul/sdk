import Foundation
import FjuulCore

public enum Gender: String, Codable {
    case male
    case female
    case other
}

public struct UserProfile: Codable {

    public let token: String
    public let birthDate: Date
    public let gender: Gender
    public let height: Int
    public let weight: Int
    public var timezone: TimeZone { return TimeZone(identifier: _timezone)! }
    public let locale: String

    enum CodingKeys: String, CodingKey {
        case token
        case birthDate
        case gender
        case height
        case weight
        case _timezone = "timezone"
        case locale
    }

    private var _timezone: String

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
        case \UserProfile.timezone: return value[\.timezone]!.identifier
        default: return value[key]!
        }
    }

}
