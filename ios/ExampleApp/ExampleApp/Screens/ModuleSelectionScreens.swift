import SwiftUI

struct ModuleSelectionScreens: View {

    var body: some View {

        Form {
            Section(header: Text("User")) {
                NavigationLink(destination: LazyView(UserProfileScreen())) {
                    Text("Profile")
                }
            }
            Section(header: Text("Analytics")) {
                NavigationLink(destination: LazyView(DailyStatsScreen())) {
                    Text("Daily Statistics")
                }
            }
        }
        .navigationBarTitle("Modules", displayMode: .inline)

    }

}

struct ModuleSelectionScreen_Previews: PreviewProvider {
    static var previews: some View {
        ModuleSelectionScreens()
    }
}
