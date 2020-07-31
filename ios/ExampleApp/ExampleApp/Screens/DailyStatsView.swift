import SwiftUI
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
        guard let apiClient = ApiClientHolder.default.apiClient else {
            print("no api client initialized")
            return
        }
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

struct DailyStatsView: View {

    @ObservedObject var dailyStats = DailyStatsObservable()

    var body: some View {
        Form {
            Section {
                DatePicker(selection: $dailyStats.fromDate, displayedComponents: .date, label: { Text("From") })
                DatePicker(selection: $dailyStats.toDate, displayedComponents: .date, label: { Text("To") })
            }
            Section(header: Text("Results")) {
                if dailyStats.isLoading {
                    Text("Loading")
                } else {
                    List(dailyStats.value, id: \.date) { each in
                        Text("Stats for \(each.date): mod \(each.moderate.metMinutes) high \(each.high.metMinutes)")
                    }
                }
            }
        }
        .alert(item: $dailyStats.error) { holder in
            Alert(title: Text(holder.error.localizedDescription))
        }
        .navigationBarTitle("Daily Stats")
    }

}

struct DailyStatsView_Previews: PreviewProvider {
    static var previews: some View {
        DailyStatsView()
    }
}
