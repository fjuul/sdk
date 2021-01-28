import Foundation

/**
 Builder for ActivitySourceConfig with default values.

 ~~~
 let config = ActivitySourceConfigBuilder { builder in
     builder.healthKitConfig = ActivitySourceHKConfig(dataTypesToRead: [
                                                         .activeEnergyBurned,
                                                         .stepCount, .workoutType,
     ])
 }
 ~~~
*/
public final class ActivitySourceConfigBuilder {
    public var healthKitConfig: ActivitySourceHKConfig = ActivitySourceHKConfig(
        dataTypesToRead: [.activeEnergyBurned, .distanceCycling, .distanceWalkingRunning, .stepCount, .workoutType, .heartRate],
        syncUserEnteredData: true
    )

    public typealias BuilderClosure = (ActivitySourceConfigBuilder) -> Void

    public init(buildClosure: BuilderClosure) {
        buildClosure(self)
    }
}
