import Foundation

public struct ValidationErrorJSONBodyResponse: ErrorJSONBodyResponsable {
    public let message: String

    public let errors: [ValidationError]
}
