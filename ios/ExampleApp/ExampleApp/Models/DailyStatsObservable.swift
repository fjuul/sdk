import Foundation
import Combine
import FjuulCore
import FjuulAnalytics

class DailyStatsObservable: ObservableObject {

    private let apiClient: ApiClient

    @Published var isLoading: Bool = false
    @Published var error: ErrorHolder?

    @Published var fromDate: Date = Date()
    @Published var toDate: Date = Date()
    @Published var value: [DailyStats] = []

    private var dateObserver: AnyCancellable?

    init(apiClient: ApiClient) {
        self.apiClient = apiClient
        dateObserver = $fromDate.combineLatest($toDate).sink { (fromDate, toDate) in
            self.fetch(fromDate, toDate)
        }
    }

    func fetch(_ fromDate: Date, _ toDate: Date) {
        self.value = []
        self.isLoading = true
        apiClient.analytics.dailyStats(from: fromDate, to: toDate) { result in
            self.isLoading = false
            switch result {
            case .success(let dailyStats):
                self.value = dailyStats.sorted(by: { $0.date.compare($1.date) == .orderedDescending })
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
        }
    }

}
