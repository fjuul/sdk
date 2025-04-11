import SwiftUI

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
                                Text("low \(each.low.metMinutes, specifier: "%.1f")")
                                Spacer()
                                Text("\(self.formatTime(time: each.low.seconds))")
                            }
                            HStack {
                                Image(systemName: "bolt")
                                    .frame(width: 30, height: 10, alignment: .leading)
                                Text("mod \(each.moderate.metMinutes, specifier: "%.1f")")
                                Spacer()
                                Text("\(self.formatTime(time: each.moderate.seconds))")
                            }
                            HStack {
                                Image(systemName: "bolt")
                                    .frame(width: 30, height: 10, alignment: .leading)
                                Text("high \(each.high.metMinutes, specifier: "%.1f")")
                                Spacer()
                                Text("\(self.formatTime(time: each.high.seconds))")
                            }
                            HStack {
                                Image(systemName: "figure.walk")
                                    .frame(width: 30, height: 10, alignment: .leading)
                                Text("steps \(each.steps, specifier: "%d")")
                            }
                        }.padding(.top, 5)
                    }
                }
            }
        }
        .alert(item: $dailyStats.error) { holder in
            Alert(title: Text(holder.error.localizedDescription))
        }
        .navigationBarTitle("Daily Stats")
    }

    func formatTime(time: TimeInterval, units: NSCalendar.Unit = [.hour, .minute]) -> String {
        let formatter = DateComponentsFormatter()
        formatter.allowedUnits = units
        formatter.unitsStyle = .abbreviated
        formatter.zeroFormattingBehavior = .pad

        return formatter.string(from: time) ?? ""
    }
}

struct DailyStatsScreen_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            DailyStatsScreen()
        }
    }
}
