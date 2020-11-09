import Foundation
import FjuulCore

protocol ActivitySourceProtocol {
    var apiClient: ApiClient? { get }

    func mount(apiClient: ApiClient, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void) -> Void
    func unmount(completion: @escaping (Result<Bool, Error>) -> Void)
}
