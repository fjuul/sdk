import SwiftUI
import FjuulAnalytics

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
                Picker("Aggregate", selection: $aggregatedStats.aggregation) {
                    ForEach(AggregationType.allCases, id: \.rawValue) { type in
                        Text(type.rawValue).tag(type)
                    }
                }.pickerStyle(SegmentedPickerStyle())
            }
            Section(header: Text("Results")) {
                if aggregatedStats.isLoading {
                    Text("Loading")
                } else {
                    let stats = aggregatedStats.value!
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Image(systemName: "bolt")
                                .frame(width: 30, height: 10, alignment: .leading)
                            Text("low \(stats.low.metMinutes)")
                            Spacer()
                            Text("\(self.formatTime(time: stats.low.seconds))")
                        }
                        HStack {
                            Image(systemName: "bolt")
                                .frame(width: 30, height: 10, alignment: .leading)
                            Text("mod \(stats.moderate.metMinutes)")
                            Spacer()
                            Text("\(self.formatTime(time: stats.moderate.seconds))")
                        }
                        HStack {
                            Image(systemName: "bolt")
                                .frame(width: 30, height: 10, alignment: .leading)
                            Text("high \(stats.high.metMinutes)")
                            Spacer()
                            Text("\(self.formatTime(time: stats.high.seconds))")
                        }
                        HStack {
                            Image(systemName: "figure.walk")
                                .frame(width: 30, height: 10, alignment: .leading)
                            Text("steps \(stats.steps)")
                        }
                    }.padding(.top, 5)
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
