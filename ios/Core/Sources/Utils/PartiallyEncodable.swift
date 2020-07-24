import Foundation

public protocol PartiallyEncodable {

    static func key(for keyPath: PartialKeyPath<Self>) -> String?
    static func jsonEncodableValue(for key: PartialKeyPath<Self>, in value: Partial<Self>) -> Any

}
