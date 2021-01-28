import Foundation
import FjuulCore
import Alamofire

/// The ActivitySource class for the Garmin tracker. This is an external activity source.
public final class ActivitySourceGarmin: ActivitySource {
    static public let shared = ActivitySourceGarmin()

    public var tracker = ActivitySourcesItem.garmin

    private init() {}
}
