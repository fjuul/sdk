import Foundation

public protocol Persistor {

    func get(key: String) -> Any?
    func set(key: String, value: Codable?)

}
