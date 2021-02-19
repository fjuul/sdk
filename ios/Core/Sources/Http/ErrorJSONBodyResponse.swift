import Foundation

protocol ErrorJSONBodyResponsible: Codable {
    var message: String { get }
}

public struct ErrorJSONBodyResponse: ErrorJSONBodyResponsible {
    public let message: String
}
