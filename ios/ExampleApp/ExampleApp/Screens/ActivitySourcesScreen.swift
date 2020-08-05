import SwiftUI

struct ActivitySourcesScreen: View {

    @ObservedObject var observable = ActivitySourceObservable()

    var body: some View {
        Form {
            Button(action: {
                self.observable.connect(activitySource: "fitbit")
            }) {
                Text("Connect Fitbit")
            }
            Button(action: {
                self.observable.connect(activitySource: "garmin")
            }) {
                Text("Connect Garmin")
            }
            Button(action: {
                self.observable.connect(activitySource: "googlefit_backend")
            }) {
                Text("Connect GoogleFit (BE Integration)")
            }
            Button(action: {
                self.observable.connect(activitySource: "polar")
            }) {
                Text("Connect Polar")
            }
            Button(action: {
                self.observable.connect(activitySource: "suunto")
            }) {
                Text("Connect Suunto")
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
