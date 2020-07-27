import SwiftUI
import FjuulCore
import FjuulUser

struct CreateUserView: View {

    @Environment(\.presentationMode) var presentation
    @EnvironmentObject var userDefaultsManager: UserDefaultsManager
    @ObservedObject var userProfile = UserProfileObservable()

    var body: some View {

        Form {
            Section {
                UserProfileForm(showOptionalFields: false).environmentObject(userProfile)
            }
            Section {
                Button("Create and apply") {
                    let profile = PartialUserProfile([
                        \UserProfile.birthDate: self.userProfile.birthDate,
                        \UserProfile.gender: self.userProfile.gender,
                        \UserProfile.height: self.userProfile.height,
                        \UserProfile.weight: self.userProfile.weight
                    ])
                    ApiClient.createUser(baseUrl: self.userDefaultsManager.environment.baseUrl, apiKey: self.userDefaultsManager.apiKey, profile: profile) { result in
                        switch result {
                        case .success(let user):
                            print(user)
                            self.userDefaultsManager.token = user.user.token
                            self.userDefaultsManager.secret = user.secret
                        case .failure(let err): print(err)
                        }
                        self.presentation.wrappedValue.dismiss()
                    }
                }
            }
        }
        .navigationBarTitle("Create User", displayMode: .inline)

    }

}

struct CreateUserView_Previews: PreviewProvider {
    static var previews: some View {
        CreateUserView()
    }
}
