import Foundation

public protocol PartiallyEncodable {

    static func keyString(for key: PartialKeyPath<Self>) -> String?
    static func jsonEncodableValueFor(for key: PartialKeyPath<Self>, with value: Partial<Self>) -> Any

}
