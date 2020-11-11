import SwiftUI
import FjuulActivitySources

struct ActivitySourcesScreen: View {

    @ObservedObject var observable = ActivitySourceObservable()

    var body: some View {
        Form {
            // TODO the proper implementation for this would be to check for each source if it is included in
            // currentConnections, as this could potentially be multiple at the same time in the future
            Text("current source: \(observable.currentConnections.first?.tracker ?? "none")")
            Button(action: {
                self.observable.connect(activitySourceItem: ActivitySourcesItem.fitbit)
            }) {
                Text("Connect Fitbit")
            }
            Button(action: {
                self.observable.connect(activitySourceItem: ActivitySourcesItem.garmin)
            }) {
                Text("Connect Garmin")
            }
            Button(action: {
                self.observable.connect(activitySourceItem: ActivitySourcesItem.googlefit_backend)
            }) {
                Text("Connect GoogleFit (BE Integration)")
            }
            Button(action: {
                self.observable.connect(activitySourceItem: ActivitySourcesItem.polar)
            }) {
                Text("Connect Polar")
            }
            Button(action: {
                self.observable.connect(activitySourceItem: ActivitySourcesItem.suunto)
            }) {
                Text("Connect Suunto")
            }

            Button(action: {
                self.observable.connect(activitySourceItem: ActivitySourcesItem.healthkit)
            }) {
                Text("Connect Healthkit")
            }
        }
        .navigationBarTitle("Activity Sources", displayMode: .inline)
        .alert(item: $observable.error) { holder in
            Alert(title: Text(holder.error.localizedDescription))
        }
    }

}

struct ActivitySourcesScreen_Previews: PreviewProvider {
    static var previews: some View {
        ActivitySourcesScreen()
    }
}
