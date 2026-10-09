import SwiftUI
import FjuulCore
import FjuulActivitySources

struct OnboardingScreen: View {

    @EnvironmentObject var userDefaultsManager: UserDefaultsManager
    @ObservedObject var viewRouter: ViewRouter
    @State private var error: ErrorHolder?

    var everythingProvidedForUserCreation: Bool {
        return userDefaultsManager.apiKey.count > 0
    }

    var everythingProvided: Bool {
        return userDefaultsManager.token.count > 0
            && userDefaultsManager.secret.count > 0
            && userDefaultsManager.apiKey.count > 0
    }

    var body: some View {
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
                NavigationLink(destination: CreateUserScreen()) {
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
                Button("Continue") {
                    FjuulApiBuilder.setUpApiClient { error in
                        if let error = error {
                            self.error = ErrorHolder(error: error)
                            return
                        }
                        self.viewRouter.presentedView = .moduleSelection
                    }
                }.disabled(!everythingProvided)
            }
        }
        .navigationBarTitle("Fjuul SDK", displayMode: .inline)
        .alert(item: $error) { holder in
            Alert(title: Text(holder.error.localizedDescription))
        }
    }

}

struct OnboardingScreen_Previews: PreviewProvider {
    static var previews: some View {
        OnboardingScreen(viewRouter: ViewRouter())
    }
}
