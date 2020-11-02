struct BatchDataPoint: Codable {
    var sourceBundleIdentifiers: [String]
    var entries: [AggregatedDataPoint]
    //    var startDate: Date // No need based on discussion with Manuel
    //    var endDate: Date // No need based on discussion with Manuel
}
