import SwiftUI

struct ModuleSelectionScreens: View {

    let session: Session

    var body: some View {

        Form {
            Section(header: Text("User")) {
                NavigationLink(destination: LazyView(UserProfileScreen(userProfile: UserProfileObservable(apiClient: session.apiClient, fetchOnInit: true)))) {
                    Text("Profile")
                }
                NavigationLink(destination: LazyView(ActivitySourcesScreen().environmentObject(session.activitySources))) {
                    Text("Activity Sources")
                }
                NavigationLink(destination: LazyView(CredentialsScreen(credentials: session.credentials))) {
                    Text("Credentials")
                }
            }
            Section(header: Text("Analytics")) {
                NavigationLink(destination: LazyView(DailyStatsScreen(dailyStats: DailyStatsObservable(apiClient: session.apiClient)))) {
                    Text("Daily Statistics")
                }
                NavigationLink(destination: LazyView(AggregatedDailyStatsScreen(
                    aggregatedStats: AggregatedDailyStatsObservable(apiClient: session.apiClient)
                ))) {
                    Text("Aggregated Daily Statistics")
                }
            }
        }
        .navigationBarTitle("Modules", displayMode: .inline)
    }

}
