import Foundation
import Alamofire
import FjuulCore

/// The `AnalyticsApi` encapsulates access to a users fitness and activity data.
public class AnalyticsApi {

    let apiClient: ApiClient
    let decoder: JSONDecoder

    /// Initializes an `AnalyticsApi` instance.
    ///
    /// You should generally not call this directly, but instead use the default instance provided on `ApiClient`.
    /// - Parameter apiClient: The `ApiClient` instance to use for API requests.
    init(apiClient: ApiClient) {
        self.apiClient = apiClient
        self.decoder = JSONDecoder()
        self.decoder.dateDecodingStrategy = .formatted(DateFormatters.yyyyMMdd)
    }

    private var baseUrl: URL? {
        return URL(string: self.apiClient.baseUrl)?.appendingPathComponent("sdk/analytics/v1")
    }

    /// Retrieves the daily activity statistics for a given day.
    ///
    /// - Parameters:
    ///   - date: The day to request daily stats for; this is the date in the users local timezone.
    ///   - completion: The code to be executed once the request has finished.
    public func dailyStats(date: Date, completion: @escaping (Result<DailyStats, Error>) -> Void) {
        let path = "/daily-stats/\(apiClient.userToken)/\(DateFormatters.yyyyMMdd.string(from: date))"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(AnalyticsApiError.invalidConfig))
        }
        apiClient.signedSession.request(url, method: .get).validate().response { response in
            let decodedResponse = response.tryMap { try self.decoder.decode(DailyStats.self, from: $0!) }
            completion(decodedResponse.result)
        }
    }

    /// Retrieves the daily activity statistics for a given day interval.
    ///
    /// - Parameters:
    ///   - from: The start of the day interval to requests daily stats for (inclusive).
    ///   - to: The end of the day interval to request daily stats for (inclusive).
    ///   - completion: The code to be executed once the request has finished.
    public func dailyStats(from: Date, to: Date, completion: @escaping (Result<[DailyStats], Error>) -> Void) {
        let path = "/daily-stats/\(apiClient.userToken)"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(AnalyticsApiError.invalidConfig))
        }
        let parameters = [
            "from": DateFormatters.yyyyMMdd.string(from: from),
            "to": DateFormatters.yyyyMMdd.string(from: to),
        ]
        apiClient.signedSession.request(url, method: .get, parameters: parameters).validate().response { response in
            let decodedResponse = response.tryMap { try self.decoder.decode([DailyStats].self, from: $0!) }
            completion(decodedResponse.result)
        }
    }

}

private var AssociatedObjectHandle: UInt8 = 0

public extension ApiClient {

    var analytics: AnalyticsApi {
        if let analyticsApi = objc_getAssociatedObject(self, &AssociatedObjectHandle) as? AnalyticsApi {
            return analyticsApi
        } else {
            let analyticsApi = AnalyticsApi(apiClient: self)
            objc_setAssociatedObject(self, &AssociatedObjectHandle, analyticsApi, .OBJC_ASSOCIATION_RETAIN)
            return analyticsApi
        }
    }

}
