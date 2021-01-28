import Foundation
import FjuulCore
import Alamofire

/// The ActivitySource class for the Suunto tracker. This is an external activity source.
public final class ActivitySourceSuunto: ActivitySource {
    static public let shared = ActivitySourceSuunto()

    public var tracker = ActivitySourcesItem.suunto

    private init() {}
}
