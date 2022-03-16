import SwiftUI
import FjuulActivitySources

struct SyncHealthKitActivitySource: View {
    @ObservedObject var healthKitSyncObservable = HealthKitSyncObservable()

    var body: some View {
        Form {
            Section {
                DatePicker(selection: $healthKitSyncObservable.fromDate, displayedComponents: [.date, .hourAndMinute], label: { Text("From") })
                DatePicker(selection: $healthKitSyncObservable.toDate, displayedComponents: [.date, .hourAndMinute], label: { Text("To") })
            }

            Section(header: Text("Intraday metrics")) {
                HStack {
                    CheckboxField(
                        id: HealthKitConfigType.activeEnergyBurned.rawValue,
                        label: "Calories",
                        size: 16,
                        textSize: 16,
                        callback: self.healthKitSyncObservable.checkboxChanged
                    )
                    CheckboxField(
                        id: HealthKitConfigType.distanceCycling.rawValue,
                        label: "Distance cycling",
                        size: 16,
                        textSize: 16,
                        callback: self.healthKitSyncObservable.checkboxChanged
                    )
                }
                HStack {
                    CheckboxField(
                        id: HealthKitConfigType.heartRate.rawValue,
                        label: "Heart rate",
                        size: 16,
                        textSize: 16,
                        callback: self.healthKitSyncObservable.checkboxChanged
                    )
                    CheckboxField(
                        id: HealthKitConfigType.distanceWalkingRunning.rawValue,
                        label: "Distance walking/running",
                        size: 16,
                        textSize: 16,
                        callback: self.healthKitSyncObservable.checkboxChanged
                    )
                }
                Section {
                    if self.healthKitSyncObservable.isLoadingIntraday {
                        Text("Loading...")
                    } else {
                        Button("Sync intraday") {
                            self.healthKitSyncObservable.syncIntradayMetrics()
                        }
                    }
                }
            }

            Section(header: Text("Daily Metrics")) {
                HStack {
                    CheckboxField(
                        id: HealthKitConfigType.stepCount.rawValue,
                        label: "Steps",
                        size: 16,
                        textSize: 16,
                        callback: self.healthKitSyncObservable.checkboxChanged
                    )
                    CheckboxField(
                        id: HealthKitConfigType.restingHeartRate.rawValue,
                        label: "Resting Heart Rate",
                        size: 16,
                        textSize: 16,
                        callback: self.healthKitSyncObservable.checkboxChanged
                    )
                }
                Section {
                    if self.healthKitSyncObservable.isLoadingDailyMetrics {
                        Text("Loading...")
                    } else {
                        Button("Sync daily metrics") {
                            self.healthKitSyncObservable.syncDailyMetrics()
                        }
                    }
                }
            }

            Section(header: Text("Workouts")) {
                Section {
                    if self.healthKitSyncObservable.isLoadingWorkouts {
                        Text("Loading...")
                    } else {
                        Button("Sync workouts") {
                            self.healthKitSyncObservable.syncWorkouts()
                        }
                    }
                }
            }

            Section(header: Text("Profile")) {
                HStack {
                    CheckboxField(
                        id: HealthKitConfigType.height.rawValue,
                        label: "Height",
                        size: 16,
                        textSize: 16,
                        callback: self.healthKitSyncObservable.checkboxChanged
                    )

                    CheckboxField(
                        id: HealthKitConfigType.weight.rawValue,
                        label: "Weight",
                        size: 16,
                        textSize: 16,
                        callback: self.healthKitSyncObservable.checkboxChanged
                    )
                }

                Section {
                    if self.healthKitSyncObservable.isLoadingProfile {
                        Text("Loading...")
                    } else {
                        Button("Sync profile") {
                            self.healthKitSyncObservable.syncProfile()
                        }
                    }
                }
            }
        }
        .alert(item: $healthKitSyncObservable.error) { holder in
            Alert(title: Text(holder.error.localizedDescription))
        }
        .navigationBarTitle("HealthKit sync")
    }
}

struct SyncHealthKitActivitySource_Previews: PreviewProvider {
    static var previews: some View {
        SyncHealthKitActivitySource()
    }
}
