import Foundation

public final class ActivitySourceConfigBuilder {
    public var healthKitConfig: ActivitySourceHKConfig = ActivitySourceHKConfig(dataTypes: [.activeEnergyBurned])

    public typealias BuilderClosure = (ActivitySourceConfigBuilder) -> ()

    public init(buildClosure: BuilderClosure) {
        buildClosure(self)
    }
}
