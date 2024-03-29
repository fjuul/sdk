import Foundation

/// The protocol for all activity source classes.
/// Like HealthKitActivitySource, PolarActivitySource, etc.
public protocol ActivitySource {
    var trackerValue: TrackerValue { get }
}

/// The protocol for mountable activity sources. Currently only HealthKit with mount backgroudDelivery.
protocol MountableActivitySource: ActivitySource {
    var apiClient: ActivitySourcesApiClient? { get }

    /// Mount activity source. As example configure and setup HealthKit backgroud delivery.
    /// - Parameters:
    ///   - apiClient: ActivitySourcesApiClient
    ///   - config: ActivitySourceConfigBuilder for configuring what kind of data types should process.
    ///   - healthKitManagerBuilder: Builder for healthKitManager
    ///   - completion: completion with void or error. If an error occurred,
    ///   this object contains information about the error (`FjuulError.activitySourceFailure.healthkitNotAvailableOnDevice`)
    func mount(apiClient: ActivitySourcesApiClient, config: ActivitySourceConfigBuilder,
               healthKitManagerBuilder: HealthKitManagerBuilding, completion: @escaping (Result<Void, Error>) -> Void)

    /// Unmount activity source and disables all background deliveries of update notifications. As example unmount HealthKit backgroud delivery.
    /// - Parameter:
    ///   - completion: completion with void or error. If an error occurred,
    ///   this object contains information about the error (`FjuulError.activitySourceFailure.healthkitNotAvailableOnDevice`,
    ///   `FjuulError.activitySourceFailure.activitySourceNotMounted`)
    func unmount(completion: @escaping (Result<Void, Error>) -> Void)
}
