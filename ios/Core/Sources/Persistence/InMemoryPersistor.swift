import Foundation

public class InMemoryPersistor: Persistor {

    var store: [String: Any] = [:]

    public init() {}

    public func set<T: Encodable>(key: String, value: T?) {
        guard let unwrapped = value else {
            store.removeValue(forKey: key)
            return
        }
        store[key] = unwrapped
    }

    public func get<T: Decodable>(key: String) -> T? {
        return store[key] as? T
    }

}
