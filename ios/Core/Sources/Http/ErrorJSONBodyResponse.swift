import Foundation

protocol ErrorJSONBodyResponsable: Codable {
    var message: String { get }
}

public struct ErrorJSONBodyResponse: ErrorJSONBodyResponsable {
    public let message: String
}
