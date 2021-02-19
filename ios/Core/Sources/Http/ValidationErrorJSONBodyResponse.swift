import Foundation

public struct ValidationErrorJSONBodyResponse: ErrorJSONBodyResponsible {
    public let message: String

    public let errors: [ValidationError]
}
