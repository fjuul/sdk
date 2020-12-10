import SwiftUI
import FjuulCore
import FjuulUser

struct CreateUserScreen: View {

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
                    self.userProfile.createNewUser(baseUrl: self.userDefaultsManager.environment.baseUrl, apiKey: self.userDefaultsManager.apiKey) { result in
                        switch result {
                        case .success(let user):
                            print(user)
                            self.userDefaultsManager.token = user.user.token
                            self.userDefaultsManager.secret = user.secret
                            self.presentation.wrappedValue.dismiss()
                        case .failure: break
                        }
                    }
                }
            }
        }
        .navigationBarTitle("Create User", displayMode: .inline)
    }

}

struct CreateUserScreen_Previews: PreviewProvider {
    static var previews: some View {
        CreateUserScreen()
    }
}
