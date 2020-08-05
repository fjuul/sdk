import Foundation
import FjuulActivitySources
import UIKit

class ActivitySourceObservable: ObservableObject {

    @Published var error: ErrorHolder?
    @Published var activitySource: String?

    func connect(activitySource: String) {
        ApiClientHolder.default.apiClient?.activitySources.connect(activitySource: activitySource) { result in
            switch result {
            case .success(let connectionResult):
                switch connectionResult {
                case .connected: break
                case .externalAuthenticationFlowRequired(let authenticationUrl):
                    guard let url = URL(string: authenticationUrl) else { return }
                    UIApplication.shared.open(url)
                }
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
        }
    }

}
