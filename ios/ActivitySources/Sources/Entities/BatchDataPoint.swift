struct BatchDataPoint: Codable {
    var sources: [String]
    var items: [AggregatedDataPoint]
    //    var startDate: Date // No need based on discussion with Manuel
    //    var endDate: Date // No need based on discussion with Manuel
}
