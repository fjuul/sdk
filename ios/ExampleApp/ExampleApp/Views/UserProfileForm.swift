import SwiftUI
import FjuulUser

struct UserProfileForm: View {

    let showOptionalFields: Bool

    @EnvironmentObject var userProfile: UserProfileObservable

    var body: some View {
        Section {
            DatePicker(selection: $userProfile.birthDate, displayedComponents: .date, label: { Text("Birthdate") })
            Stepper(value: $userProfile.height, in: 100...250, step: 0.5) {
                Text(String(format: "Height: %.1fcm", userProfile.height))
            }
            Stepper(value: $userProfile.weight, in: 35...250, step: 0.5) {
                Text(String(format: "Weight: %.1fkg", userProfile.weight))
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
                FloatingTextField(title: "Locale", text: $userProfile.locale)
                    .disableAutocorrection(true)
                    .autocapitalization(.none)
            }
        }
        .alert(item: $userProfile.error) { holder in
            Alert(title: Text(holder.error.localizedDescription))
        }
    }

}

struct UserProfileForm_Previews: PreviewProvider {
    static var previews: some View {
        UserProfileForm(showOptionalFields: true)
    }
}
