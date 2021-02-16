import Foundation

public enum ValidationErrorValue: Codable, Equatable {
    case double(Double)
    case string(String)
    case int(Int)

    public init(from decoder: Decoder) throws {
        let container =  try decoder.singleValueContainer()

        if let intVal = try? container.decode(Int.self) {
            self = .int(intVal)
        } else if let doubleVal = try? container.decode(Double.self) {
            self = .double(doubleVal)
        } else {
            let stringVal = try container.decode(String.self)
            self = .string(stringVal)
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .double(let value):
            try container.encode(value)
        case .int(let value):
            try container.encode(value)
        case .string(let value):
            try container.encode(value)
        }
    }
}

public struct ValidationError: Codable {
    public let property: String
    public let constraints: [String: String]
    public let value: ValidationErrorValue
}
