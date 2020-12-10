import SwiftUI
import FjuulCore
import FjuulActivitySources

struct OnboardingScreen: View {

    @EnvironmentObject var userDefaultsManager: UserDefaultsManager
    @ObservedObject var viewRouter: ViewRouter

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
                    if ApiClientHolder.default.apiClient == nil {
                        ApiClientHolder.default.apiClient = ApiClient(
                            baseUrl: self.userDefaultsManager.environment.baseUrl,
                            apiKey: self.userDefaultsManager.apiKey,
                            credentials: UserCredentials(
                                token: self.userDefaultsManager.token,
                                secret: self.userDefaultsManager.secret
                            )
                        )

                        let config = ActivitySourceConfigBuilder { builder in
                            builder.healthKitConfig = ActivitySourceHKConfig(dataTypesToRead: [.heartRate, .activeEnergyBurned,
                                                                                               .distanceCycling, .distanceWalkingRunning, .stepCount, .workoutType])
                        }

                        if let apiClient = ApiClientHolder.default.apiClient {
                            ActivitySourceManager.shared.initialize(apiClient: apiClient, config: config)
                        }
                    }
                    self.viewRouter.presentedView = .moduleSelection
                }.disabled(!everythingProvided)
            }
        }
        .navigationBarTitle("Fjuul SDK", displayMode: .inline)
    }

}

struct OnboardingScreen_Previews: PreviewProvider {
    static var previews: some View {
        OnboardingScreen(viewRouter: ViewRouter())
    }
}
