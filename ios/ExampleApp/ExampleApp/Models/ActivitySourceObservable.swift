import Foundation
import FjuulActivitySources
import UIKit

class ActivitySourceObservable: ObservableObject {
    let availableActivitySources: [ActivitySource] = [
        HealthKitActivitySource.shared, FitbitActivitySource.shared, GarminActivitySource.shared,
        OuraActivitySource.shared, PolarActivitySource.shared,
        SuuntoActivitySource.shared, WithingsActivitySource.shared
    ]

    @Published var error: ErrorHolder?
    @Published var currentConnections: [ActivitySourceConnection] = []
    @Published var notConnectedActivitySources: [ActivitySource] = []

    init() {
        self.getCurrentConnections()
    }

    func currentConnectionsLabels() -> String {
        let labels = self.currentConnections.compactMap { item in item.tracker.value }

        if labels.count > 0 {
            return labels.joined(separator: ", ")
        }

        return "none"
    }

    func getCurrentConnections() {
        ApiClientHolder.default.apiClient?.activitySourcesManager?.refreshCurrent { result in
            switch result {
            case .success(let connections):
                self.currentConnections = connections
                self.notConnectedActivitySources = self.availableActivitySources.filter { item in
                    return !connections.contains { connection in connection.tracker == item.trackerValue }
                }
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
        }
    }

    func connect(activitySource: ActivitySource) {
        ApiClientHolder.default.apiClient?.activitySourcesManager?.connect(activitySource: activitySource) { result in
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
        ApiClientHolder.default.apiClient?.activitySourcesManager?.disconnect(activitySourceConnection: activitySourceConnection) { result in
            switch result {
            case .success: self.getCurrentConnections()
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
        }
    }

    func disconnectAll() {
        let connections = self.currentConnections
        if connections.isEmpty { return }
        for connection in connections {
            self.disconnect(activitySourceConnection: connection)
        }
    }
}
