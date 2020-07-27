import SwiftUI
import FjuulUser

class UserProfileObservable: ObservableObject {

    @Published var error: ErrorHolder?

    @Published var token = ""
    @Published var birthDate = Date(timeIntervalSince1970: 0)
    @Published var height = 170
    @Published var weight = 80
    @Published var gender: Gender = .other
    @Published var timezone = ""
    @Published var locale: String?

    init(fetchOnInit: Bool = false) {
        if fetchOnInit {
            fetch()
        }
    }

    func hydrateFromUserProfile(_ profile: UserProfile) {
        self.token = profile.token
        self.birthDate = profile.birthDate
        self.height = profile.height
        self.weight = profile.weight
        self.gender = profile.gender
        self.timezone = profile.timezone
        self.locale = profile.locale
    }

    func fetch() {
        guard let apiClient = ApiClientHolder.default.apiClient else {
            print("no api client initialized")
            return
        }
        apiClient.user.getProfile { result in
            switch result {
            case .success(let profile):
                print(profile)
                self.hydrateFromUserProfile(profile)
            case .failure(let err):
                print(err)
                self.error = ErrorHolder(error: err)
            }
        }
    }

}

struct UserProfileForm: View {

    let showOptionalFields: Bool

    @EnvironmentObject var userProfile: UserProfileObservable

    var body: some View {
        Section {
            DatePicker(selection: $userProfile.birthDate, displayedComponents: .date, label: { Text("Birthdate") })
            Stepper(value: $userProfile.height, in: 100...250, step: 5) {
                Text("Height: \(userProfile.height)cm")
            }
            Stepper(value: $userProfile.weight, in: 35...250, step: 5) {
                Text("Weight: \(userProfile.weight)kg")
            }
            Picker(selection: $userProfile.gender, label: Text("Gender"), content: {
                Text("male").tag(Gender.male)
                Text("female").tag(Gender.female)
                Text("other").tag(Gender.other)
            })
            if showOptionalFields {
                FloatingTextField(title: "Timezone", text: $userProfile.timezone)
                    .disableAutocorrection(true)
                    .autocapitalization(.none)
            }
        }
    }

}

struct UserProfileForm_Previews: PreviewProvider {
    static var previews: some View {
        UserProfileForm(showOptionalFields: true)
    }
}
