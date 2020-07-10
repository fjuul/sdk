import SwiftUI

struct MainView: View {

    var body: some View {

        // NavigationView {
            Form {
                Section {
                    NavigationLink(destination: MainView()) {
                        Text("Analytics")
                    }
                }
            }
            .navigationBarTitle("Modules", displayMode: .inline)
        // }

    }

}

struct MainView_Previews: PreviewProvider {
    static var previews: some View {
        MainView()
    }
}
