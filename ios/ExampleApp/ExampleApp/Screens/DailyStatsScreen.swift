import SwiftUI

struct DailyStatsScreen: View {

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

struct DailyStatsScreen_Previews: PreviewProvider {
    static var previews: some View {
        DailyStatsScreen()
    }
}
