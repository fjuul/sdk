import Foundation
import FjuulCore

public enum Gender: String, Codable {
    case male
    case female
    case other
}

struct UserProfileCodingOptions {
    let json: [String: Any]?

    static let key = CodingUserInfoKey(rawValue: "com.fjuul.sdk.user-profile")!
}

enum UserProfileCodingError: Error {
    case noJSONData
    case invalidHeightValue
    case invalidWeightValue
}

public struct UserProfile: Codable {
    public let token: String
    public let birthDate: Date
    public let gender: Gender
    public let height: Decimal
    public let weight: Decimal
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

    public init(from decoder: Decoder) throws {
        guard let options = decoder.userInfo[UserProfileCodingOptions.key] as? UserProfileCodingOptions else {
            throw UserProfileCodingError.noJSONData
        }
        let container = try decoder.container(keyedBy: CodingKeys.self)
        token = try container.decode(String.self, forKey: .token)
        birthDate = try container.decode(Date.self, forKey: .birthDate)
        gender = try container.decode(Gender.self, forKey: .gender)
        guard let jsonHeight = options.json?["height"] else {
            throw UserProfileCodingError.invalidHeightValue
        }
        height = Decimal(string: String(describing: jsonHeight))!
        guard let jsonWeight = options.json?["weight"] else {
            throw UserProfileCodingError.invalidWeightValue
        }
        weight = Decimal(string: String(describing: jsonWeight))!
        _timezone = try container.decode(String.self, forKey: ._timezone)
        locale = try container.decode(String.self, forKey: .locale)
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
