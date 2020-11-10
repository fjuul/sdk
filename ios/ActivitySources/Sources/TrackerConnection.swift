import Foundation

//public enum TrackerConnectionTypes: String, Encodable {
//    case fitbit, garmin, googlefit_backend, polar, suunto, healthkit, none
//}

// TODO: Make TrackerConnection private
public struct TrackerConnection: Codable {

    public let id: String
    public let tracker: String
    public let createdAt: Date
    public let endedAt: Date?

}
