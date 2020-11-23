import Foundation

public final class ActivitySourceConfigBuilder {
    public var healthKitConfig: ActivitySourceHKConfig = ActivitySourceHKConfig(
        dataTypesToRead: [.activeEnergyBurned, .distanceCycling, .distanceWalkingRunning, .stepCount],
        syncUserEnteredData: true
    )

    public typealias BuilderClosure = (ActivitySourceConfigBuilder) -> Void

    public init(buildClosure: BuilderClosure) {
        buildClosure(self)
    }
}
