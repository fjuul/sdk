import Foundation
import FjuulCore

/// The ActivitySource class for the Polar tracker. This is an external activity source.
public final class ActivitySourcePolar: ActivitySource {
    static public let shared = ActivitySourcePolar()

    public var tracker = ActivitySourcesItem.polar

    private init() {}
}
