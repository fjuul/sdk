import Foundation

public class InMemoryPersistor: Persistor {

    var store: [String: Any] = [:]

    public init() {}

    public func set(key: String, value: Codable?) {
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
