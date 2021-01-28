import Foundation
import FjuulCore
import Alamofire

/// The ActivitySource class for the Fitbit tracker. This is an external activity source.
public final class ActivitySourceFitbit: ActivitySource {
    static public let shared = ActivitySourceFitbit()

    public var tracker = ActivitySourcesItem.fitbit

    private init() {}
}
