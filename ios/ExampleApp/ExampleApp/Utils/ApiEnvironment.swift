import Foundation

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

