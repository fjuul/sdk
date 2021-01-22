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
            }.disabled(self.connected(tracker: .fitbit))

            Button(action: {
                self.activitySourceObserver.connect(activitySource: ActivitySourceGarmin.shared)
            }) {
                Text("Connect Garmin")
            }.disabled(self.connected(tracker: .garmin))

            Button(action: {
                self.activitySourceObserver.connect(activitySource: ActivitySourcePolar.shared)
            }) {
                Text("Connect Polar")
            }.disabled(self.connected(tracker: .polar))

            Button(action: {
                self.activitySourceObserver.connect(activitySource: ActivitySourceSuunto.shared)
            }) {
                Text("Connect Suunto")
            }.disabled(self.connected(tracker: .suunto))

            Button(action: {
                self.activitySourceObserver.connect(activitySource: ActivitySourceHK.shared)
            }) {
                Text("Connect Healthkit")
            }.disabled(self.connected(tracker: .healthkit))
        }
        .navigationBarTitle("Activity Sources", displayMode: .inline)
        .alert(item: $activitySourceObserver.error) { holder in
            Alert(title: Text(holder.error.localizedDescription))
        }
    }

    private func connected(tracker: ActivitySourcesItem) -> Bool {
        return self.activitySourceObserver.currentConnections.contains { activitySourceConnection in
            return activitySourceConnection.tracker == tracker
        }
    }

}

struct ActivitySourcesScreen_Previews: PreviewProvider {
    static var previews: some View {
        ActivitySourcesScreen()
    }
}
