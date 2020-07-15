import Foundation

public protocol Persistor {

    func get(key: String) -> Any?
    mutating func set(key: String, value: Codable?)

}
