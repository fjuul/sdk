import Foundation
import Alamofire
import FjuulCore

public class AnalyticsApi {

    let apiClient: ApiClient
    let dateFormatter: DateFormatter
    let decoder: JSONDecoder

    init(apiClient: ApiClient) {
        self.apiClient = apiClient
        self.dateFormatter = DateFormatter()
        self.dateFormatter.dateFormat = "yyyy-MM-dd"
        self.decoder = JSONDecoder()
        self.decoder.dateDecodingStrategy = .formatted(self.dateFormatter)
    }

    private var baseUrl: URL? {
        return URL(string: self.apiClient.baseUrl)?.appendingPathComponent("sdk/analytics/v1")
    }

    public func dailyStats(date: Date, completion: @escaping (Result<DailyStats, Error>) -> Void) {
        let path = "/daily-stats/\(apiClient.userToken)/\(dateFormatter.string(from: date))"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(AnalyticsApiError.invalidConfig))
        }
        apiClient.signedSession.request(url, method: .get).response { response in
            switch response.result {
            case .success(let data):
                do {
                    let dailyStats = try self.decoder.decode(DailyStats.self, from: data!)
                    completion(.success(dailyStats))
                } catch {
                    completion(.failure(error))
                }
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }

    public func dailyStats(from: Date, to: Date, completion: @escaping (Result<[DailyStats], Error>) -> Void) {
        let path = "/daily-stats/\(apiClient.userToken)"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(AnalyticsApiError.invalidConfig))
        }
        let parameters = [
            "from": dateFormatter.string(from: from),
            "to": dateFormatter.string(from: to),
        ]
        apiClient.signedSession.request(url, method: .get, parameters: parameters).response { response in
            switch response.result {
            case .success(let data):
                do {
                    let dailyStats = try self.decoder.decode([DailyStats].self, from: data!)
                    completion(.success(dailyStats))
                } catch {
                    completion(.failure(error))
                }
            case .failure(let error):
                completion(.failure(error))
            }
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
