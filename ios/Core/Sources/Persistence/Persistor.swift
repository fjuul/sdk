import Foundation

public protocol Persistor {

    func get<T: Decodable>(key: String) -> T?
    func set<T: Encodable>(key: String, value: T?)
    func remove(matchKey: String) -> Bool
}
