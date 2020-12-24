import Foundation
import FjuulCore
import Alamofire

public final class ActivitySourceSuunto: ActivitySource {
    static public let shared = ActivitySourceSuunto()

    public var tracker = ActivitySourcesItem.suunto

    private init() {}
}
