import SwiftUI
import FjuulActivitySources

struct ActivitySourcesScreen: View {

    @ObservedObject var observable = ActivitySourceObservable()

    var body: some View {
        Form {
            // TODO the proper implementation for this would be to check for each source if it is included in
            // currentConnections, as this could potentially be multiple at the same time in the future
            Text("current source: \(observable.currentConnections.first?.tracker?.rawValue ?? "none")")
            Button(action: {
                self.observable.connect(activitySource: ActivitySourceFitbit.shared)
            }) {
                Text("Connect Fitbit")
            }
            Button(action: {
                self.observable.connect(activitySource: ActivitySourceGarmin.shared)
            }) {
                Text("Connect Garmin")
            }
//            Button(action: {
//                self.observable.connect(activitySource: ActivitySourcesItem.googlefit_backend)
//            }) {
//                Text("Connect GoogleFit (BE Integration)")
//            }
            Button(action: {
                self.observable.connect(activitySource: ActivitySourcePolar.shared)
            }) {
                Text("Connect Polar")
            }
            Button(action: {
                self.observable.connect(activitySource: ActivitySourceSuunto.shared)
            }) {
                Text("Connect Suunto")
            }

            Button(action: {
                self.observable.connect(activitySource: ActivitySourceHK.shared)
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
