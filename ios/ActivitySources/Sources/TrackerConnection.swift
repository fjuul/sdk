import Foundation

public struct TrackerConnection: Codable {

    public let id: String
    public let tracker: String
    public let createdAt: Date
    public let endedAt: Date?

}
