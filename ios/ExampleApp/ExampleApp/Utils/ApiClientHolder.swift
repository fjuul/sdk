import Foundation
import FjuulCore

struct ApiClientHolder {

    static var `default` = Self()
    var apiClient: ApiClient?

}
