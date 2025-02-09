import SwiftUI
import FjuulActivitySources

struct ActivitySourcesScreen: View {
    @EnvironmentObject var activitySourceObserver: ActivitySourceObservable

    var body: some View {
        Form {
            Text("Current sources: \(activitySourceObserver.currentConnectionsLabels())")
            ForEach(self.activitySourceObserver.currentConnections, id: \.self.id) { activitySourceConnection in
                HStack {
                    Button(action: {
                        self.activitySourceObserver.disconnect(activitySourceConnection: activitySourceConnection)
                    }) {
                        Text("Disconnect \(activitySourceConnection.tracker.value)")
                    }.buttonStyle(BorderlessButtonStyle())

                    if activitySourceConnection.tracker == TrackerValue.HEALTHKIT {
                        Spacer()

                        ZStack {
                            NavigationLink(destination: LazyView(SyncHealthKitActivitySource())) {
                                EmptyView()
                            }
                            .buttonStyle(PlainButtonStyle())
                            .opacity(0.0)

                            HStack {
                                Spacer()
                                Text("Sync")
                            }
                        }
                    }
                }
            }

            Section {
                ForEach(self.activitySourceObserver.notConnectedActivitySources, id: \.self.trackerValue.value) { activitySource in
                    Button(action: {
                        self.activitySourceObserver.connect(activitySource: activitySource)
                    }) {
                        Text("Connect \(activitySource.trackerValue.value)")
                    }
                }
            }
        }
        .navigationBarTitle("Activity Sources", displayMode: .inline)
        .alert(item: $activitySourceObserver.error) { holder in
            Alert(title: Text(holder.error.localizedDescription))
        }
    }
}

struct ActivitySourcesScreen_Previews: PreviewProvider {
    static var previews: some View {
        ActivitySourcesScreen()
    }
}
