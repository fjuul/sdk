import Foundation

public struct InMemoryPersistor: Persistor {

    var store: [String: Any] = [:]

    public init() {}

    public mutating func set(key: String, value: Codable) {
        store[key] = value
    }

    public func get(key: String) -> Any? {
        return store[key]
    }

}
