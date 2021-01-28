import Foundation
import FjuulCore

/// Wraper for the local store with enabled activity sources.
/// Persisted local store need for mount mountable ActivitySources on boot app in AppDelegate, see Example app.
public class ActivitySourceStore {
    private let lookupKey: String
    private var persistor: Persistor

    init(userToken: String, persistor: Persistor) {
        self.persistor = persistor
        self.lookupKey = "tracker-connections.\(userToken)"
    }

    var connections: [TrackerConnection]? {
        get {
            return persistor.get(key: lookupKey)
        }
        set {
            persistor.set(key: lookupKey, value: newValue)
        }
    }
}
