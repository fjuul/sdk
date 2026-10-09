import Foundation
import FjuulActivitySources
import UIKit

/// Activity source state of one `Session`; discarded with it, so results arriving after sign-out never reach the UI.
class ActivitySourceObservable: ObservableObject {
    let availableActivitySources: [ActivitySource] = [
        HealthKitActivitySource.shared, FitbitActivitySource.shared,
        GarminActivitySource.shared, GoogleHealthActivitySource.shared,
        OuraActivitySource.shared, PolarActivitySource.shared,
        SuuntoActivitySource.shared, WithingsActivitySource.shared
    ]

    let manager: ActivitySourcesManager

    @Published var error: ErrorHolder?
    @Published var currentConnections: [ActivitySourceConnection] = []
    @Published var notConnectedActivitySources: [ActivitySource] = []

    init(manager: ActivitySourcesManager) {
        self.manager = manager
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
        manager.refreshCurrent { result in
            DispatchQueue.main.async {
                switch result {
                case .success(let connections):
                    self.setConnections(connections)
                case .failure(let err): self.error = ErrorHolder(error: err)
                }
            }
        }
    }

    func loadLocalConnections() {
        setConnections(manager.mountedActivitySourceConnections)
    }

    private func setConnections(_ connections: [ActivitySourceConnection]) {
        self.currentConnections = connections
        self.notConnectedActivitySources = self.availableActivitySources.filter { item in
            return !connections.contains { connection in connection.tracker == item.trackerValue }
        }
    }

    func connect(activitySource: ActivitySource) {
        manager.connect(activitySource: activitySource) { result in
            DispatchQueue.main.async {
                switch result {
                case .success(let connectionResult):
                    switch connectionResult {
                    case .connected: self.getCurrentConnections()
                    case .externalAuthenticationFlowRequired(let authenticationUrl):
                        // Unlike UI updates, opening the browser is visible after sign-out and would connect the previous user.
                        guard SessionStore.shared.isActive(self.manager),
                              let url = URL(string: authenticationUrl) else { return }
                        UIApplication.shared.open(url)
                    }
                case .failure(let err): self.error = ErrorHolder(error: err)
                }
            }
        }
    }

    func disconnect(activitySourceConnection: ActivitySourceConnection) {
        manager.disconnect(activitySourceConnection: activitySourceConnection) { result in
            DispatchQueue.main.async {
                switch result {
                case .success: self.getCurrentConnections()
                case .failure(let err): self.error = ErrorHolder(error: err)
                }
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
