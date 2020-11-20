import Foundation

public final class ActivitySourceConfigBuilder {
    public var healthKitConfig: ActivitySourceHKConfig = ActivitySourceHKConfig(dataTypesToRead: [.activeEnergyBurned])

    public typealias BuilderClosure = (ActivitySourceConfigBuilder) -> ()

    public init(buildClosure: BuilderClosure) {
        buildClosure(self)
    }
}
