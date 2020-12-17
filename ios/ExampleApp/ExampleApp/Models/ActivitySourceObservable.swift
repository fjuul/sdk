import Foundation
import FjuulActivitySources
import UIKit

class ActivitySourceObservable: ObservableObject {
    @Published var error: ErrorHolder?
    @Published var currentConnections: [ActivitySourceConnection] = []

    init() {
        self.getCurrentConnections()
    }

    func getCurrentConnections() {
        ActivitySourceManager.current?.getCurrentConnections { result in
            switch result {
            case .success(let connections): self.currentConnections = connections
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
        }
    }

    func connect(activitySource: ActivitySource) {
        ActivitySourceManager.current?.connect(activitySource: activitySource) { result in
            switch result {
            case .success(let connectionResult):
                switch connectionResult {
                case .connected: self.getCurrentConnections()
                case .externalAuthenticationFlowRequired(let authenticationUrl):
                    guard let url = URL(string: authenticationUrl) else { return }
                    UIApplication.shared.open(url)
                }
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
        }
    }

    func disconnect(activitySourceConnection: ActivitySourceConnection) {
        ActivitySourceManager.current?.disconnect(activitySourceConnection: activitySourceConnection) { result in
            switch result {
            case .success: self.getCurrentConnections()
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
        }
    }

}
