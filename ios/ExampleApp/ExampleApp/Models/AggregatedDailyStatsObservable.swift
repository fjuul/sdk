import Foundation
import Combine
import FjuulCore
import FjuulAnalytics

class AggregatedDailyStatsObservable: ObservableObject {

    private let apiClient: ApiClient

    @Published var isLoading: Bool = false
    @Published var error: ErrorHolder?

    @Published var fromDate: Date = Date()
    @Published var toDate: Date = Date()
    @Published var aggregation: AggregationType = .sum
    @Published var value: AggregatedDailyStats?

    private var dateObserver: AnyCancellable?

    init(apiClient: ApiClient) {
        self.apiClient = apiClient
        dateObserver = $fromDate.combineLatest($toDate, $aggregation).sink { (fromDate, toDate, aggregation) in
            self.fetch(fromDate, toDate, aggregation)
        }
    }

    func fetch(_ fromDate: Date, _ toDate: Date, _ aggregation: AggregationType) {
        self.isLoading = true
        apiClient.analytics.aggregatedDailyStats(from: fromDate, to: toDate, aggregation: aggregation) { result in
            self.isLoading = false
            switch result {
            case .success(let aggregatedDailyStats): self.value = aggregatedDailyStats
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
        }
    }

}
