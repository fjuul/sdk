import Foundation

public protocol Persistor: Sendable {
    static func remove(matchKey: String) -> Bool

    func get<T: Decodable>(key: String) -> T?
    func set<T: Encodable>(key: String, value: T?)
    func remove(key: String) -> Bool
}
