import SwiftUI

struct RootView: View {

    @EnvironmentObject var sessionStore: SessionStore

    var body: some View {
        NavigationView {
            if let session = sessionStore.session {
                // A new id rebuilds all screens (and their models) for every session.
                ModuleSelectionScreens(session: session).id(session.id)
            } else {
                OnboardingScreen()
            }
        }
        .alert(item: $sessionStore.error) { holder in
            Alert(title: Text(holder.error.localizedDescription))
        }
    }

}

struct RootView_Previews: PreviewProvider {
    static var previews: some View {
        RootView()
    }
}
