import SwiftUI
import FjuulCore
import FjuulUser

struct CreateUserView: View {

    @Environment(\.presentationMode) var presentation
    @EnvironmentObject var userDefaultsManager: UserDefaultsManager

    @State private var birthDate = Date(timeIntervalSince1970: 0)
    @State private var height = 170
    @State private var weight = 80
    @State private var gender = Gender.other

    var body: some View {

        Form {
            Section {
                DatePicker(selection: $birthDate, displayedComponents: .date, label: { Text("Birthdate") })
                Stepper(value: $height, in: 100...250, step: 5) {
                    Text("Height: \(height)cm")
                }
                Stepper(value: $weight, in: 35...250, step: 5) {
                    Text("Weight: \(weight)kg")
                }
                Picker(selection: $gender, label: Text("Gender"), content: {
                    Text("male").tag(Gender.male)
                    Text("female").tag(Gender.female)
                    Text("other").tag(Gender.other)
                })
            }
            Section {
                Button("Create and apply") {
                    let profile = PartialUserProfile([
                        \UserProfile.birthDate: self.birthDate,
                        \UserProfile.gender: self.gender,
                        \UserProfile.height: self.height,
                        \UserProfile.weight: self.weight
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
