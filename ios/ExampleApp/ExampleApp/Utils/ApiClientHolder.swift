import Foundation
import FjuulCore

struct ApiClientHolder {

    static var `default` = Self()
    private(set) var config: FjuulApiBuilder.ApiClientConfig?
    private(set) var apiClient: ApiClient? {
        didSet { NotificationCenter.default.post(name: .apiClientDidChange, object: nil) }
    }

    mutating func set(_ apiClient: ApiClient?, config: FjuulApiBuilder.ApiClientConfig?) {
        self.config = config
        self.apiClient = apiClient
    }

}

extension Notification.Name {
    static let apiClientDidChange = Notification.Name("apiClientDidChange")
}
