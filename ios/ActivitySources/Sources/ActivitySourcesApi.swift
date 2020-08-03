import Foundation
import Alamofire
import FjuulCore

/// The `ActivitySourcesApi` encapsulates access to a users fitness and activity data.
public class ActivitySourcesApi {

    let apiClient: ApiClient

    /// Initializes an `ActivitySourcesApi` instance.
    ///
    /// You should generally not call this directly, but instead use the default instance provided on `ApiClient`.
    /// - Parameter apiClient: The `ApiClient` instance to use for API requests.
    init(apiClient: ApiClient) {
        self.apiClient = apiClient
    }

    private var baseUrl: URL? {
        return URL(string: self.apiClient.baseUrl)?.appendingPathComponent("sdk/activity-sources/v1")
    }

}

private var AssociatedObjectHandle: UInt8 = 0

public extension ApiClient {

    var activitySources: ActivitySourcesApi {
        if let activitySourcesApi = objc_getAssociatedObject(self, &AssociatedObjectHandle) as? ActivitySourcesApi {
            return activitySourcesApi
        } else {
            let activitySourcesApi = ActivitySourcesApi(apiClient: self)
            objc_setAssociatedObject(self, &AssociatedObjectHandle, activitySourcesApi, .OBJC_ASSOCIATION_RETAIN)
            return activitySourcesApi
        }
    }

}
