import Foundation
import Alamofire
import FjuulCore

public enum ConnectionResult {
    case externalAuthenticationFlowRequired(authenticationUrl: String)
    case connected(trackerConnection: TrackerConnection)
}
protocol AutoMockable { }

protocol ActivitySourcesApiClient: AutoMockable {
    var apiClient: ApiClient { get }

    func connect(trackerValue: TrackerValue, completion: @escaping (Result<ConnectionResult, Error>) -> Void)
    func disconnect(activitySourceConnection: ActivitySourceConnection, completion: @escaping (Result<Void, Error>) -> Void)
    func getCurrentConnections(completion: @escaping (Result<[TrackerConnection], Error>) -> Void)
    func sendHealthKitUserProfileData(data: HKUserProfileData, completion: @escaping (Result<Void, Error>) -> Void)
    func sendHealthKitBatchData(data: HKBatchData, completion: @escaping (Result<Void, Error>) -> Void)
    func sendHealthKitDailyMetrics(data: [HKDailyMetricDataPoint], completion: @escaping (Result<Void, Error>) -> Void)
}

/// The `ActivitySourcesApi` encapsulates the management of a users activity sources.
public class ActivitySourcesApi: ActivitySourcesApiClient {

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

    func connect(trackerValue: TrackerValue, completion: @escaping (Result<ConnectionResult, Error>) -> Void) {
        let path = "/\(apiClient.userToken)/connections/\(trackerValue.value)"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(FjuulError.invalidConfig))
        }
        apiClient.signedSession.request(url, method: .post).apiResponse { response in
            let mappedResponse = response
                .tryMap { result -> ConnectionResult in
                    switch response.response?.statusCode {
                    case 200:
                        let json = try JSONSerialization.jsonObject(with: result, options: []) as? [String: Any]
                        guard let authenticationUrl = json?["url"] as? String else {
                            throw FjuulError.activitySourceConnectionFailure(reason: .generic(message: nil))
                        }
                        return .externalAuthenticationFlowRequired(authenticationUrl: authenticationUrl)
                    case 201:
                        let trackerConnection = try Decoders.iso8601Full.decode(TrackerConnection.self, from: result)
                        return .connected(trackerConnection: trackerConnection)
                    default: throw FjuulError.activitySourceConnectionFailure(reason: .generic(message: nil))
                    }
                }
                .mapAPIError { resp, jsonError in
                    guard let jsonError = jsonError else { return nil }

                    if resp.response?.statusCode == 409 {
                        return .activitySourceConnectionFailure(reason: .sourceAlreadyConnected(message: jsonError.message))
                    }

                    return .activitySourceConnectionFailure(reason: .generic(message: jsonError.message))
                }
            completion(mappedResponse.result)
        }
    }

    func disconnect(activitySourceConnection: ActivitySourceConnection, completion: @escaping (Result<Void, Error>) -> Void) {
        let path = "/\(apiClient.userToken)/connections/\(activitySourceConnection.id)"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(FjuulError.invalidConfig))
        }
        apiClient.signedSession.request(url, method: .delete).apiResponse { response in
            let decodedResponse = response
                .map { _ in () }
                .mapAPIError { _, jsonError in
                    guard let jsonError = jsonError else { return nil }

                    return .activitySourceConnectionFailure(reason: .generic(message: jsonError.message))
                }
            completion(decodedResponse.result)
        }
    }

    func getCurrentConnections(completion: @escaping (Result<[TrackerConnection], Error>) -> Void) {
        let path = "/\(apiClient.userToken)/connections"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(FjuulError.invalidConfig))
        }
        let parameters = ["show": "current"]
        apiClient.signedSession.request(url, method: .get, parameters: parameters).apiResponse { response in
            let decodedResponse = response
                .tryMap { try Decoders.iso8601Full.decode([TrackerConnection].self, from: $0) }
                .mapAPIError { _, jsonError in
                    guard let jsonError = jsonError else { return nil }

                    return .activitySourceConnectionFailure(reason: .generic(message: jsonError.message))
                }
            completion(decodedResponse.result)
        }
    }

    internal func sendHealthKitBatchData(data: HKBatchData, completion: @escaping (Result<Void, Error>) -> Void) {
        let path = "/\(apiClient.userToken)/healthkit"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(FjuulError.invalidConfig))
        }

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .formatted(DateFormatters.iso8601Full)
        let parameterEncoder = JSONParameterEncoder(encoder: encoder)

        apiClient.signedSession.request(url, method: .post, parameters: data, encoder: parameterEncoder).apiResponse(emptyResponseCodes: [200]) { response in
            let decodedResponse = response.map { _ in () }
            completion(decodedResponse.result)
        }
    }

    internal func sendHealthKitDailyMetrics(data: [HKDailyMetricDataPoint], completion: @escaping (Result<Void, Error>) -> Void) {
        let path = "/\(apiClient.userToken)/healthkit/dailies"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(FjuulError.invalidConfig))
        }

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .formatted(DateFormatters.yyyyMMddLocale)
        let parameterEncoder = JSONParameterEncoder(encoder: encoder)

        apiClient.signedSession.request(url, method: .post, parameters: data, encoder: parameterEncoder).apiResponse(emptyResponseCodes: [200]) { response in
            let decodedResponse = response.map { _ in () }
            completion(decodedResponse.result)
        }
    }

    internal func sendHealthKitUserProfileData(data: HKUserProfileData, completion: @escaping (Result<Void, Error>) -> Void) {
        let path = "/\(apiClient.userToken)/healthkit/profile"
        guard let url = baseUrl?.appendingPathComponent(path) else {
            return completion(.failure(FjuulError.invalidConfig))
        }

        apiClient.signedSession.request(url, method: .put, parameters: data.asJsonEncodableDictionary(), encoding: JSONEncoding.default)
            .apiResponse(emptyResponseCodes: [200]) { response in

            let decodedResponse = response
                .map { _ in () }
                .mapAPIError { _, jsonError in
                    guard let jsonError = jsonError else { return nil }

                    return .activitySourceConnectionFailure(reason: .generic(message: jsonError.message))
                }
            completion(decodedResponse.result)
        }
    }
}

private var AssociatedObjectHandle: UInt8 = 0

public extension ApiClient {
    var activitySourcesManager: ActivitySourcesManager? {
        get {
            if let manager = objc_getAssociatedObject(self, &AssociatedObjectHandle) as? ActivitySourcesManager {
                return manager
            } else {
                return nil
            }
        }
        set {
            objc_setAssociatedObject(self, &AssociatedObjectHandle, newValue, .OBJC_ASSOCIATION_RETAIN)
        }
    }

    /// Initialize the initActivitySourcesManager with the provided config.
    /// Should be Initialize once as soon as possible after up app, for setup backgroundDelivery for the HealthKit to fetch intraday data,
    /// for example in AppDelegate (didFinishLaunchingWithOptions)
    /// - Parameter config: ActivitySourceConfigBuilder config with desire config for list of Data types for sync.
    /// - Parameter completion: Optional completion block will be called when the local state is restored
    func initActivitySourcesManager(config: ActivitySourceConfigBuilder, completion: ((Result<Void, Error>) -> Void)? = nil) {
        self.activitySourcesManager = ActivitySourcesManager(
            userToken: self.userToken,
            persistor: self.persistor,
            apiClient: ActivitySourcesApi(apiClient: self),
            config: config,
            completion: completion
        )
    }

}
