import Foundation
import FjuulCore

public protocol ActivitySource {
    var apiClient: ApiClient? { get }
    /// TODO: Add Enum for tracker names
    var tracker: String { get }

    func mount(apiClient: ApiClient, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void)
    func unmount(completion: @escaping (Result<Bool, Error>) -> Void)
}
