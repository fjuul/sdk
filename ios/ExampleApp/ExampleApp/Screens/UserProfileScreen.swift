import SwiftUI

struct UserProfileScreen: View {

    @Environment(\.presentationMode) var presentation
    @ObservedObject var userProfile = UserProfileObservable(fetchOnInit: true)

    var body: some View {

        Form {
            Section {
                UserProfileForm(showOptionalFields: true).environmentObject(userProfile)
            }
            Section {
                Button("Update profile") {
                    self.userProfile.updateUser { result in
                        switch result {
                        case .success: self.presentation.wrappedValue.dismiss()
                        case .failure: break
                        }
                    }
                }
            }
        }
        .navigationBarTitle("User Profile", displayMode: .inline)

    }

}

struct UserProfileScreen_Previews: PreviewProvider {
    static var previews: some View {
        UserProfileScreen()
    }
}
