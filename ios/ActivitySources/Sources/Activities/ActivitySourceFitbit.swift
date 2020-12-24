import Foundation
import FjuulCore
import Alamofire

public final class ActivitySourceFitbit: ActivitySource {
    static public let shared = ActivitySourceFitbit()

    public var tracker = ActivitySourcesItem.fitbit

    private init() {}
}
