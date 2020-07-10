import Foundation

class UserDefaultsManager: ObservableObject {

    @Published var environment: ApiEnvironment = ApiEnvironment(rawValue: UserDefaults.standard.integer(forKey: "environment")) ?? .test {
        didSet { UserDefaults.standard.set(self.environment.rawValue, forKey: "environment") }
    }

    @Published var apiKey: String = UserDefaults.standard.string(forKey: "apiKey") ?? "" {
        didSet { UserDefaults.standard.set(self.apiKey, forKey: "apiKey") }
    }

    @Published var token: String = UserDefaults.standard.string(forKey: "token") ?? "" {
        didSet { UserDefaults.standard.set(self.token, forKey: "token") }
    }

    @Published var secret: String = UserDefaults.standard.string(forKey: "secret") ?? "" {
        didSet { UserDefaults.standard.set(self.secret, forKey: "secret") }
    }

}
