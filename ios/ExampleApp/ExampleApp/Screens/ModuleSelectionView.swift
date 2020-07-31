import SwiftUI

struct ModuleSelectionView: View {

    var body: some View {

        Form {
            Section(header: Text("Analytics")) {
                NavigationLink(destination: LazyView(DailyStatsView())) {
                    Text("Daily Statistics")
                }
            }
        }
        .navigationBarTitle("Modules", displayMode: .inline)

    }

}

struct ModuleSelectionView_Previews: PreviewProvider {
    static var previews: some View {
        ModuleSelectionView()
    }
}
