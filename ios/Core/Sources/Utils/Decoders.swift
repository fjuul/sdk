import Foundation

public struct Decoders {

    public static let iso8601Full: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .formatted(DateFormatters.iso8601Full)
        return decoder
    }()

    public static let yyyyMMdd: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .formatted(DateFormatters.yyyyMMdd)
        return decoder
    }()

}
