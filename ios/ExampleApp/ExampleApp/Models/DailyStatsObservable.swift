import Foundation
import Combine
import FjuulAnalytics

class DailyStatsObservable: ObservableObject {

    @Published var isLoading: Bool = false
    @Published var error: ErrorHolder?

    @Published var fromDate: Date = Date(timeIntervalSinceNow: -7 * 24 * 60 * 60)
    @Published var toDate: Date = Date()
    @Published var value: [DailyStats] = []

    private var dateObserver: AnyCancellable?

    init() {
        dateObserver = $fromDate.combineLatest($toDate).sink { (fromDate, toDate) in
            self.fetch(fromDate, toDate)
        }
    }

    func fetch(_ fromDate: Date, _ toDate: Date) {
        self.value = []
        self.isLoading = true
        ApiClientHolder.default.apiClient?.analytics.dailyStats(from: fromDate, to: toDate) { result in
            self.isLoading = false
            switch result {
            case .success(let dailyStats):
                self.value = dailyStats.sorted(by: { $0.date.compare($1.date) == .orderedDescending })
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
        }
    }

}
