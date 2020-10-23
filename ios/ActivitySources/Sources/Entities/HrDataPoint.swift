import Foundation

struct HrDataPoint {
    var uuid: UUID
    var value: Int // Unit is count/min
    var startDate: Date
    var endDate: Date
    var source: [String]? // bundleIdentifier
//    var device: // TODO: Add correct DataType
    var metadata: [String : Any]?
}
