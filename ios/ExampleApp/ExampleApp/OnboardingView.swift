import SwiftUI

enum ApiEnvironment: Int, CaseIterable, Identifiable {

    case development
    case test
    case production

    var label: String {
        switch self {
        case .development: return "development"
        case .test: return "test"
        case .production: return "⚠️ production"
        }
    }

    var baseUrl: String {
        switch self {
        case .development: return "https://dev.api.fjuul.com"
        case .test: return "https://test.api.fjuul.com"
        case .production: return "https://api.fjuul.com"
        }
    }

    var id: ApiEnvironment { self }

}

struct OnboardingView: View {

    @EnvironmentObject var userDefaultsManager: UserDefaultsManager

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
