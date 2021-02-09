import Foundation

public struct ValidationError: Codable {
    public let property: String
    public let value: String
    public let constraints: [String: String]
}
