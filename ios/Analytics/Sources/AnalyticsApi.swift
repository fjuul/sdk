import Foundation
import Alamofire
import FjuulCore

public class AnalyticsApi {

    let apiClient: ApiClient
    let dateFormatter: DateFormatter

    init(apiClient: ApiClient) {
        self.apiClient = apiClient
        self.dateFormatter = DateFormatter()
        self.dateFormatter.dateFormat = "yyyy-MM-dd"
    }

    private var baseUrl: String { get { return "\(self.apiClient.baseUrl)/sdk/analytics/v1" } }

    func dailyStats(date: Date, completion: @escaping (DailyStats?, Error?) -> Void) {
        let path = "/daily-stats/\(apiClient.userToken)/\(dateFormatter.string(from: date))"
        apiClient.signedSession.request("\(baseUrl)\(path)", method: .get).response { response in
            switch response.result {
            case .success(let data):
                do {
                    let decoder = JSONDecoder()
                    decoder.dateDecodingStrategy = .formatted(self.dateFormatter)
                    let dailyStats = try decoder.decode(DailyStats.self, from: data!)
                    completion(dailyStats, nil)
                } catch {
                    completion(nil, error)
                }
            case .failure(let error):
                completion(nil, error)
            }
        }
    }

    func dailyStats(from: Date, to: Date) {

    }

}

private var AssociatedObjectHandle: UInt8 = 0

public extension ApiClient {

    var analytics: AnalyticsApi {
        get {
            if let analyticsApi = objc_getAssociatedObject(self, &AssociatedObjectHandle) as? AnalyticsApi {
                return analyticsApi
            } else {
                let analyticsApi = AnalyticsApi(apiClient: self)
                objc_setAssociatedObject(self, &AssociatedObjectHandle, analyticsApi, .OBJC_ASSOCIATION_RETAIN)
                return analyticsApi
            }
        }
    }

}
