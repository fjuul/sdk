import Foundation
import HealthKit

public struct HKAnchorKey: CustomStringConvertible, Codable, Equatable {

    let id: String
    let anchor: Data

    public var description: String {
        return "Anchor: \(id)"
    }

}
