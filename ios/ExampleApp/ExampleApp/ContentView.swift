import SwiftUI
import FjuulAnalytics

class DailyStatsObservable: ObservableObject {
    @Published var value: DailyStats?
    init() {
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else {
            debugPrint("error retrieving AppDelegate reference")
            return
        }
        appDelegate.apiClient?.analytics.dailyStats(date: Date()) { result in
            switch result {
            case .success(let dailyStats): self.value = dailyStats
            case .failure(let err): debugPrint(err)
            }
        }
    }
}

struct ContentView: View {
    @ObservedObject var dailyStats = DailyStatsObservable()
    var body: some View {
        guard let stats = dailyStats.value else {
            return Text("waiting for data")
        }
        return Text("Stats for \(stats.date): mod \(stats.moderate.metMinutes) high \(stats.high.metMinutes)")
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
