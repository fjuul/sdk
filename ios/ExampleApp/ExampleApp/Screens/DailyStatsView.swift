import SwiftUI
import FjuulAnalytics

class DailyStatsObservable: ObservableObject {

    @Published var value: [DailyStats] = []

    init() {
        guard let apiClient = ApiClientHolder.default.apiClient else {
            print("no api client initialized")
            return
        }
        apiClient.analytics.dailyStats(from: Date(), to: Date()) { result in
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
        List(dailyStats.value, id: \.date) { each in
            Text("Stats for \(each.date): mod \(each.moderate.metMinutes) high \(each.high.metMinutes)")
        }
    }

}

struct DailyStatsView_Previews: PreviewProvider {
    static var previews: some View {
        DailyStatsView()
    }
}
