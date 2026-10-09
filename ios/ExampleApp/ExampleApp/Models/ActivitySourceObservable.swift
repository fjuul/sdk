import Foundation
import Combine
import FjuulActivitySources
import UIKit

class ActivitySourceObservable: ObservableObject {
    let availableActivitySources: [ActivitySource] = [
        HealthKitActivitySource.shared, FitbitActivitySource.shared,
        GarminActivitySource.shared, GoogleHealthActivitySource.shared,
        OuraActivitySource.shared, PolarActivitySource.shared,
        SuuntoActivitySource.shared, WithingsActivitySource.shared
    ]

    @Published var error: ErrorHolder?
    @Published var currentConnections: [ActivitySourceConnection] = []
    @Published var notConnectedActivitySources: [ActivitySource] = []

    private var apiClientChangeSubscription: AnyCancellable?

    init() {
        self.getCurrentConnections()
        // This observer outlives sign-in and sign-out, so it has to reload whenever the api client is replaced.
        apiClientChangeSubscription = NotificationCenter.default.publisher(for: .apiClientDidChange)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in self?.getCurrentConnections() }
    }

    func currentConnectionsLabels() -> String {
        let labels = self.currentConnections.compactMap { item in item.tracker.value }

        if labels.count > 0 {
            return labels.joined(separator: ", ")
        }

        return "none"
    }

    func getCurrentConnections() {
        guard let manager = ApiClientHolder.default.apiClient?.activitySourcesManager else {
            self.currentConnections = []
            self.notConnectedActivitySources = []
            return
        }
        manager.refreshCurrent { result in
            DispatchQueue.main.async {
                // A refresh started before logout or a client switch can finish after it.
                guard manager === ApiClientHolder.default.apiClient?.activitySourcesManager else { return }
                switch result {
                case .success(let connections):
                    self.setConnections(connections)
                case .failure(let err): self.error = ErrorHolder(error: err)
                }
            }
        }
    }

    func loadLocalConnections() {
        guard let manager = ApiClientHolder.default.apiClient?.activitySourcesManager else { return }
        setConnections(manager.mountedActivitySourceConnections)
    }

    private func setConnections(_ connections: [ActivitySourceConnection]) {
        self.currentConnections = connections
        self.notConnectedActivitySources = self.availableActivitySources.filter { item in
            return !connections.contains { connection in connection.tracker == item.trackerValue }
        }
    }

    func connect(activitySource: ActivitySource) {
        guard let manager = ApiClientHolder.default.apiClient?.activitySourcesManager else { return }
        manager.connect(activitySource: activitySource) { result in
            DispatchQueue.main.async {
                // Opening a stale authentication URL would connect a tracker for the previous user.
                guard manager === ApiClientHolder.default.apiClient?.activitySourcesManager else { return }
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
    }

    func disconnect(activitySourceConnection: ActivitySourceConnection) {
        guard let manager = ApiClientHolder.default.apiClient?.activitySourcesManager else { return }
        manager.disconnect(activitySourceConnection: activitySourceConnection) { result in
            DispatchQueue.main.async {
                guard manager === ApiClientHolder.default.apiClient?.activitySourcesManager else { return }
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
