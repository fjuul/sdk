import Foundation

protocol TrackerConnectionable {
    var id: String { get }
    var createdAt: Date { get }
    var endedAt: Date? { get }
}

// TODO make TrackerConnection protected
public struct TrackerConnection: TrackerConnectionable, Codable {

    public let id: String
    public let tracker: String
    public let createdAt: Date
    public let endedAt: Date?

}
