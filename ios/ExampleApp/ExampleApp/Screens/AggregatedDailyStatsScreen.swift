import SwiftUI

struct AggregatedDailyStatsScreen: View {

    @ObservedObject var aggregatedStats = AggregatedDailyStatsObservable()

    static let taskDateFormat: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .long
        return formatter
    }()

    var body: some View {
        Form {
            Section {
                DatePicker(selection: $aggregatedStats.fromDate, displayedComponents: .date, label: { Text("From") })
                DatePicker(selection: $aggregatedStats.toDate, displayedComponents: .date, label: { Text("To") })
                
            }
            Section(header: Text("Results")) {
                if aggregatedStats.isLoading {
                    Text("Loading")
                } else {
                    List(aggregatedStats.value, id: \.date) { each in
                        VStack(alignment: .leading, spacing: 4) {
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
                                Image(systemName: "flame")
                                    .frame(width: 30, height: 10, alignment: .leading)
                                Text("activeKcal \(each.activeKcal, specifier: "%.1f")")
                            }
                            HStack {
                                Image(systemName: "bolt.heart")
                                    .frame(width: 30, height: 10, alignment: .leading)
                                Text("bmr \(each.bmr, specifier: "%.1f")")
                            }
                        }.padding(.top, 5)
                    }
                }
            }
        }
        .alert(item: $aggregatedStats.error) { holder in
            Alert(title: Text(holder.error.localizedDescription))
        }
        .navigationBarTitle("Aggregated Daily Stats")
    }

    func formatTime(time: TimeInterval, units: NSCalendar.Unit = [.hour, .minute]) -> String {
        let formatter = DateComponentsFormatter()
        formatter.allowedUnits = units
        formatter.unitsStyle = .abbreviated
        formatter.zeroFormattingBehavior = .pad

        return formatter.string(from: time) ?? ""
    }
}

struct AggregatedDailyStatsScreen_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            AggregatedDailyStatsScreen()
        }
    }
}
