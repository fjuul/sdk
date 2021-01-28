import Foundation

/// Base protocol for TrackerConnection and ActivitySourceConnection
protocol TrackerConnectionable {
    var id: String { get }
    var createdAt: Date { get }
    var endedAt: Date? { get }
}

/// A class that represents information about the connected activity source.
public struct TrackerConnection: TrackerConnectionable, Codable {

    public let id: String
    public let tracker: String
    public let createdAt: Date
    public let endedAt: Date?

}
