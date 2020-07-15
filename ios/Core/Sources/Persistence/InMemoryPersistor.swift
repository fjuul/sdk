import Foundation

public struct InMemoryPersistor: Persistor {

    var store: [String: Any] = [:]

    public init() {}

    public mutating func set(key: String, value: Codable?) {
        guard let unwrapped = value else {
            store.removeValue(forKey: key)
            return
        }
        store[key] = unwrapped
    }

    public func get(key: String) -> Any? {
        return store[key]
    }

}
