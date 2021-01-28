import SwiftUI
import FjuulActivitySources

struct ActivitySourcesScreen: View {
    @EnvironmentObject var activitySourceObserver: ActivitySourceObservable

    var body: some View {
        Form {
            Text("current source: \(activitySourceObserver.currentConnectionsLabels())")
            Button(action: {
                self.activitySourceObserver.connect(activitySource: ActivitySourceFitbit.shared)
            }) {
                Text("Connect Fitbit")
            }.disabled(self.connected(trackerValue: TrackerValue.FITBIT))

            Button(action: {
                self.activitySourceObserver.connect(activitySource: ActivitySourceGarmin.shared)
            }) {
                Text("Connect Garmin")
            }.disabled(self.connected(trackerValue: TrackerValue.GARMIN))

            Button(action: {
                self.activitySourceObserver.connect(activitySource: ActivitySourcePolar.shared)
            }) {
                Text("Connect Polar")
            }.disabled(self.connected(trackerValue: TrackerValue.POLAR))

            Button(action: {
                self.activitySourceObserver.connect(activitySource: ActivitySourceSuunto.shared)
            }) {
                Text("Connect Suunto")
            }.disabled(self.connected(trackerValue: TrackerValue.SUUNTO))

            Button(action: {
                self.activitySourceObserver.connect(activitySource: ActivitySourceHK.shared)
            }) {
                Text("Connect Healthkit")
            }.disabled(self.connected(trackerValue: TrackerValue.HEALTHKIT))
        }
        .navigationBarTitle("Activity Sources", displayMode: .inline)
        .alert(item: $activitySourceObserver.error) { holder in
            Alert(title: Text(holder.error.localizedDescription))
        }
    }

    private func connected(trackerValue: TrackerValue) -> Bool {
        return self.activitySourceObserver.currentConnections.contains { activitySourceConnection in
            return activitySourceConnection.tracker == trackerValue
        }
    }

}

struct ActivitySourcesScreen_Previews: PreviewProvider {
    static var previews: some View {
        ActivitySourcesScreen()
    }
}
