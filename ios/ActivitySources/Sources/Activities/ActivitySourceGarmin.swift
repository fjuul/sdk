import Foundation
import FjuulCore
import Alamofire

public final class ActivitySourceGarmin: ActivitySource {
    static public let shared = ActivitySourceGarmin()

    public var tracker = ActivitySourcesItem.garmin

    private init() {}
}
