import Foundation

public struct Partial<Wrapped: PartiallyEncodable> {

    private var values: [PartialKeyPath<Wrapped>: Encodable]

    public init(initialValues: [PartialKeyPath<Wrapped>: Encodable] = [:]) {
        self.values = initialValues
    }

    public subscript<ValueType: Encodable>(key: KeyPath<Wrapped, ValueType>) -> ValueType? {
        get {
            return values[key] as? ValueType
        }
        set {
            values[key] = newValue
        }
    }

    public subscript(key: PartialKeyPath<Wrapped>) -> Encodable? {
        get {
            return values[key]
        }
    }

    public func asJsonEncodableDictionary() -> [String: Any] {
        return Dictionary(uniqueKeysWithValues: values
            .filter { key, value in Wrapped.keyString(for: key) != nil }
            .map { key, value in (Wrapped.keyString(for: key)!, Wrapped.jsonEncodableValueFor(for: key, with: self)) })
    }

}
