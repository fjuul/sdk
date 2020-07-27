import SwiftUI
import FjuulUser

struct UserProfileView: View {

    @ObservedObject var userProfile = UserProfileObservable(fetchOnInit: true)

    var body: some View {

        Form {
            Section {
                UserProfileForm(showOptionalFields: true).environmentObject(userProfile)
            }
            Section {
                Button("Update profile") {
                    let updatedValues = PartialUserProfile([
                        \UserProfile.weight: self.userProfile.weight
                    ])
                    ApiClientHolder.default.apiClient?.user.updateProfile(updatedValues) { result in
                        print(result)
                    }
                }
            }
        }
        .navigationBarTitle("User Profile", displayMode: .inline)

    }

}

struct UserProfileView_Previews: PreviewProvider {
    static var previews: some View {
        UserProfileView()
    }
}
