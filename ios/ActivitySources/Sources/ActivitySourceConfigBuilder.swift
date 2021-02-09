import Foundation

/**
 Builder for ActivitySourceConfig with default values.

 ~~~
 let config = ActivitySourceConfigBuilder { builder in
     builder.healthKitConfig = HealthKitActivitySourceConfig(dataTypesToRead: [
                                                         .activeEnergyBurned,
                                                         .stepCount, .workoutType,
     ])
 }
 ~~~
*/
public final class ActivitySourceConfigBuilder {
    public var healthKitConfig: HealthKitActivitySourceConfig = HealthKitActivitySourceConfig(
        dataTypesToRead: [.activeEnergyBurned, .distanceCycling, .distanceWalkingRunning, .stepCount, .workout, .heartRate],
        syncUserEnteredData: true
    )

    public typealias BuilderClosure = (ActivitySourceConfigBuilder) -> Void

    public init(buildClosure: BuilderClosure) {
        buildClosure(self)
    }
}
