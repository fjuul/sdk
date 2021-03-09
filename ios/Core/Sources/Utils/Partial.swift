import Foundation

public struct Partial<Wrapped: PartiallyEncodable> {

    private var values: [PartialKeyPath<Wrapped>: Encodable]

    public init() {
        self.values = [:]
    }

    public init(block: (inout Self) -> Void) {
        self.init()
        block(&self)
    }

    public subscript<ValueType: Encodable>(key: KeyPath<Wrapped, ValueType>) -> ValueType? {
        get {
            return values[key] as? ValueType
        }
        set {
            values[key] = newValue
        }
    }

    public subscript(key: PartialKeyPath<Wrapped>) -> Encodable? { return values[key] }

    public func asJsonEncodableDictionary() -> [String: Any] {
        return Dictionary(uniqueKeysWithValues: values
            .filter { keyPath, _ in Wrapped.key(for: keyPath) != nil }
            .map { keyPath, _ in (Wrapped.key(for: keyPath)!, Wrapped.jsonEncodableValue(for: keyPath, in: self)) })
    }

}
