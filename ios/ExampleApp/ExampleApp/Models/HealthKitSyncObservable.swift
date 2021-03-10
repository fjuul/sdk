import Foundation
import FjuulActivitySources

class HealthKitSyncObservable: ObservableObject {
    static var dafaultEndOfDay: Date {
        var components = DateComponents()
        components.day = 1
        components.second = -1
        return Calendar.current.date(byAdding: components, to: Calendar.current.startOfDay(for: Date()))!
    }

    @Published var error: ErrorHolder?
    @Published var isLoadingIntraday: Bool = false
    @Published var isLoadingProfile: Bool = false
    @Published var isLoadingWorkouts: Bool = false

    @Published var fromDate: Date = Calendar.current.startOfDay(for: Date())
    @Published var toDate: Date = HealthKitSyncObservable.dafaultEndOfDay

    var enabledConfigTypes: Set<HealthKitConfigType> = Set(HealthKitConfigType.allCases)

    func syncIntradayMetrics() {
        guard ApiClientHolder.default.apiClient?.activitySourcesManager?
                .mountedActivitySourceConnections.first(where: { item in item.activitySource is HealthKitActivitySource }) != nil else {
            return
        }

        self.isLoadingIntraday = true

        let configTypes = HealthKitConfigType.intradayTypes.filter { item in enabledConfigTypes.contains(item) }

        HealthKitActivitySource.shared.syncIntradayMetrics(startDate: self.fromDate, endDate: self.toDate, configTypes: configTypes) { result in
            switch result {
            case .success:
                print("Success sync \(configTypes)")
            case .failure(let err): self.error = ErrorHolder(error: err)
            }

            DispatchQueue.main.async { self.isLoadingIntraday = false }
        }
    }

    func syncProfile() {
        guard ApiClientHolder.default.apiClient?.activitySourcesManager?
                .mountedActivitySourceConnections.first(where: { item in item.activitySource is HealthKitActivitySource }) != nil else {
            return
        }

        self.isLoadingProfile = true

        let configTypes = HealthKitConfigType.userProfileTypes.filter { item in enabledConfigTypes.contains(item) }

        HealthKitActivitySource.shared.syncProfile(configTypes: configTypes) { result in
            switch result {
            case .success:
                print("Success sync \(configTypes)")
            case .failure(let err): self.error = ErrorHolder(error: err)
            }

            DispatchQueue.main.async { self.isLoadingProfile = false }
        }
    }

    func syncWorkouts() {
        guard ApiClientHolder.default.apiClient?.activitySourcesManager?
                .mountedActivitySourceConnections.first(where: { item in item.activitySource is HealthKitActivitySource }) != nil else {
            return
        }

        self.isLoadingWorkouts = true

        HealthKitActivitySource.shared.syncWorkouts(startDate: self.fromDate, endDate: self.toDate) { result in
            switch result {
            case .success:
                print("Success sync workouts")
            case .failure(let err): self.error = ErrorHolder(error: err)
            }

            DispatchQueue.main.async { self.isLoadingWorkouts = false }
        }
    }

    func checkboxChanged(id: Int, isMarked: Bool) {
        if let configType = HealthKitConfigType(rawValue: id) {
            if isMarked {
                enabledConfigTypes.insert(configType)
            } else {
                enabledConfigTypes.remove(configType)
            }
        }
    }
}
