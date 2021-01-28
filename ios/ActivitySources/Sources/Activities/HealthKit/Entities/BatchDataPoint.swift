/// Batch data structure with data for 1 hour and entries agggegated by 1 min
struct BatchDataPoint: Codable {
    var sourceBundleIdentifiers: [String]
    var entries: [AggregatedDataPoint]
}

/// Batch data structure with data for 1 hour and entries agggegated by 1 min
struct HrBatchDataPoint: Codable {
    var sourceBundleIdentifiers: [String]
    var entries: [HeartRateDataPoint]
}
