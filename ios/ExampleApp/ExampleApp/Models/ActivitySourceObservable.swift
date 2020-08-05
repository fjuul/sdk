import Foundation
import FjuulActivitySources
import UIKit

class ActivitySourceObservable: ObservableObject {

    @Published var error: ErrorHolder?
    @Published var currentConnections: [TrackerConnection] = []

    init() {
        self.getCurrentConnections()
    }

    func getCurrentConnections() {
        ApiClientHolder.default.apiClient?.activitySources.getCurrentConnections { result in
            switch result {
            case .success(let connections): self.currentConnections = connections
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
        }
    }

    func connect(activitySource: String) {
        // TODO this should trigger a connection list refresh after success or when returning to the app
        // from the browser
        ApiClientHolder.default.apiClient?.activitySources.connect(activitySource: activitySource) { result in
            switch result {
            case .success(let connectionResult):
                switch connectionResult {
                case .connected(let connection): self.currentConnections = [connection]
                case .externalAuthenticationFlowRequired(let authenticationUrl):
                    guard let url = URL(string: authenticationUrl) else { return }
                    UIApplication.shared.open(url)
                }
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
        }
    }

}
