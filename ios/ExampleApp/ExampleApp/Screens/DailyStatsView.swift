import SwiftUI
import Combine
import FjuulAnalytics

class DailyStatsObservable: ObservableObject {

    @Published var fromDate: Date = Date(timeIntervalSinceNow: -7 * 24 * 60 * 60)
    @Published var toDate: Date = Date()
    @Published var value: [DailyStats] = []

    private var dateObserver: AnyCancellable?

    init() {
        dateObserver = $fromDate.merge(with: $toDate).debounce(for: 0.2, scheduler: DispatchQueue.main).sink { _ in
            self.fetch()
        }
    }

    func fetch() {
        guard let apiClient = ApiClientHolder.default.apiClient else {
            print("no api client initialized")
            return
        }
        apiClient.analytics.dailyStats(from: fromDate, to: toDate) { result in
            switch result {
            case .success(let dailyStats): self.value = dailyStats
            case .failure(let err): debugPrint(err)
            }
        }
    }

}

struct DailyStatsView: View {

    @ObservedObject var dailyStats = DailyStatsObservable()

    var body: some View {
        Form {
            Section {
                DatePicker(selection: $dailyStats.fromDate, displayedComponents: .date, label: { Text("From") })
                DatePicker(selection: $dailyStats.toDate, displayedComponents: .date, label: { Text("To") })
            }
            Section(header: Text("Results")) {
                List(dailyStats.value, id: \.date) { each in
                    Text("Stats for \(each.date): mod \(each.moderate.metMinutes) high \(each.high.metMinutes)")
                }
            }
        }.navigationBarTitle("Daily Stats")
    }

}

struct DailyStatsView_Previews: PreviewProvider {
    static var previews: some View {
        DailyStatsView()
    }
}
