import Foundation
import FjuulCore

/// The protocol for all activity source classes.
public protocol ActivitySource {
    var tracker: ActivitySourcesItem { get }
}

/// The protocol for mountable activity sources. Currently only HealthKit with mount backgroudDelivery.
protocol MountableActivitySource: ActivitySource {
    var apiClient: ActivitySourcesApiClient? { get }
    
    /// Mount activity source. As example configure and setup HealthKit backgroud delivery.
    /// - Parameters:
    ///   - apiClient: ActivitySourcesApiClient
    ///   - config: ActivitySourceConfigBuilder for configure what kind of data types should process.
    ///   - persistor: Persistor for save HealthKit anchors
    ///   - completion: completion with status or error
    func mount(apiClient: ActivitySourcesApiClient, config: ActivitySourceConfigBuilder, persistor: Persistor, healthKitManagerBuilder: HealthKitManagerBuilder, completion: @escaping (Result<Bool, Error>) -> Void)

    /// Unmount activity source. As example unmount HealthKit backgroud delivery
    /// - Parameter completion with status or error:
    func unmount(completion: @escaping (Result<Bool, Error>) -> Void)
}
