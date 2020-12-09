import SwiftUI
import FjuulAnalytics
import FjuulCore

struct DailyStatsScreen: View {

    @ObservedObject var dailyStats = DailyStatsObservable()

    static let taskDateFormat: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .long
        return formatter
    }()

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
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Image(systemName: "calendar")
                                    .frame(width: 30, height: 10, alignment: .leading)
                                Text("\(each.date, formatter: Self.taskDateFormat)")
                                    .frame(height: 10, alignment: .leading)
                            }
                            HStack {
                                Image(systemName: "bolt")
                                    .frame(width: 30, height: 10, alignment: .leading)
                                Text("mod \(each.moderate.metMinutes, specifier: "%.1f")")
                            }
                            HStack {
                                Image(systemName: "bolt")
                                    .frame(width: 30, height: 10, alignment: .leading)
                                Text("high \(each.high.metMinutes, specifier: "%.1f")")
                            }
                            HStack {
                                Image(systemName: "flame")
                                    .frame(width: 30, height: 10, alignment: .leading)
                                Text("activeKcal \(each.activeKcal, specifier: "%.1f")")
                            }
                            HStack {
                                Image(systemName: "tortoise")
                                    .frame(width: 30, height: 10, alignment: .leading)
                                Text("steps \(each.steps, specifier: "%.1f")")
                            }
                        }
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
    static var dailyStats: DailyStatsObservable {
        let json = """
            [{
                \"date\":\"2020-12-02\",\"lowest\":{\"seconds\":34200,\"metMinutes\":714.58},
                \"low\":{\"seconds\":3480,\"metMinutes\":137.97},
                \"moderate\":{\"seconds\":3900,\"metMinutes\":231.4},
                \"high\":{\"seconds\":60,\"metMinutes\":6.28},\"activeKcal\":982.53,\"totalKcal\":0,\"steps\":0}]
        """

        let item = DailyStatsObservable()

        item.isLoading = true
//        let jsonData = json.data(using: .utf8)!
//        do {
//            let value1: [DailyStats] = try Decoders.yyyyMMddLocale.decode([DailyStats].self, from: jsonData)
//            item.value = value1
//        } catch let error {
//            item.error = ErrorHolder(error: error)
//        }

        return item
    }

    static var previews: some View {
        Group {
            DailyStatsScreen().environmentObject(self.dailyStats)
        }
    }
}
