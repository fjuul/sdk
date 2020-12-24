import Foundation
import FjuulCore

public final class ActivitySourcePolar: ActivitySource {
    static public let shared = ActivitySourcePolar()

    public var tracker = ActivitySourcesItem.polar

    private init() {}
}
