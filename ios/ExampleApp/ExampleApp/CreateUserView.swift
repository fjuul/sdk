import SwiftUI

struct CreateUserView: View {

    @Environment(\.presentationMode) var presentation
    @EnvironmentObject var userDefaultsManager: UserDefaultsManager

    @State private var birthDate = Date(timeIntervalSince1970: 0)
    @State private var height = 170
    @State private var weight = 80
    @State private var gender = 0

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
                    Text("male").tag(0)
                    Text("female").tag(1)
                    Text("other").tag(2)
                })
            }
            Section {
                Button("Create and apply") {
                    // TODO actually create user and inject result here
                    self.userDefaultsManager.token = "foobar"
                    self.presentation.wrappedValue.dismiss()
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
