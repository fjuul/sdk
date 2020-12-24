struct BatchDataPoint: Codable {
    var sourceBundleIdentifiers: [String]
    var entries: [AggregatedDataPoint]
}

struct HrBatchDataPoint: Codable {
    var sourceBundleIdentifiers: [String]
    var entries: [HeartRateDataPoint]
}
