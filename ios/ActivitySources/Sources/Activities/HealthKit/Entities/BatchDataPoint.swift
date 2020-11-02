struct BatchDataPoint: Codable {
    var sourceBundleIdentifiers: [String]
    var entries: [AggregatedDataPoint]
}
