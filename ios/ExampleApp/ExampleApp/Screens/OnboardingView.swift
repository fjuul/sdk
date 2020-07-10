import SwiftUI

struct OnboardingView: View {

    @EnvironmentObject var userDefaultsManager: UserDefaultsManager
    @EnvironmentObject var apiClientHolder: ApiClientHolder

    var everythingProvidedForUserCreation: Bool {
        return userDefaultsManager.apiKey.count > 0
    }

    var everythingProvided: Bool {
        return userDefaultsManager.token.count > 0
            && userDefaultsManager.secret.count > 0
            && userDefaultsManager.apiKey.count > 0
    }

    var body: some View {

        NavigationView {
            Form {
                Section(header: Text("Environment")) {
                    Picker(selection: $userDefaultsManager.environment, label: Text("Environment")) {
                        ForEach(ApiEnvironment.allCases) { env in
                            Text(env.label).tag(env)
                        }
                    }.pickerStyle(SegmentedPickerStyle())
                    FloatingTextField(title: "API key", text: $userDefaultsManager.apiKey)
                        .disableAutocorrection(true)
                        .autocapitalization(.none)
                    NavigationLink(destination: CreateUserView()) {
                        Button("Create new user") {}
                    }.disabled(!everythingProvidedForUserCreation)
                }
                Section(header: Text("User")) {
                    FloatingTextField(title: "Token", text: $userDefaultsManager.token)
                        .disableAutocorrection(true)
                        .autocapitalization(.none)
                    FloatingTextField(title: "Secret", text: $userDefaultsManager.secret)
                        .disableAutocorrection(true)
                        .autocapitalization(.none)
                }
                Section {
                    NavigationLink(destination: MainView()) {
                        Button("Continue") {}
                    }.disabled(!everythingProvided)
                }
            }
            .navigationBarTitle("Fjuul SDK", displayMode: .inline)
        }

    }

}

struct OnboardingView_Previews: PreviewProvider {
    static var previews: some View {
        OnboardingView()
    }
}
