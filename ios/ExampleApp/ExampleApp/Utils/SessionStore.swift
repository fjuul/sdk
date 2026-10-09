import Foundation
import FjuulActivitySources

/// Owns the signed-in `Session`. Signing in is only possible while signed out, and signing in and out never overlap
/// (`isBusy`), so a session is never replaced while screens still use it.
final class SessionStore: ObservableObject {

    static let shared = SessionStore()

    @Published private(set) var session: Session?
    @Published private(set) var isBusy = false
    @Published var error: ErrorHolder?

    /// Whether results from `manager` may still trigger side effects beyond its own screens: false once its session
    /// has been replaced or while signing out (the session is only cleared after sign-out completes).
    func isActive(_ manager: ActivitySourcesManager?) -> Bool {
        return manager != nil && session?.activitySourcesManager === manager && !isBusy
    }

    /// Signs in with the credentials stored by onboarding. Must be called on the main queue.
    func signIn() {
        guard session == nil, !isBusy, let credentials = Credentials.stored() else { return }
        isBusy = true
        Session.start(credentials: credentials) { session, error in
            self.session = session
            self.isBusy = false
            self.error = error.map { ErrorHolder(error: $0) }
        }
    }

    /// Must be called on the main queue.
    func signOut() {
        guard let session = session, !isBusy else { return }
        isBusy = true
        session.end { error in
            self.isBusy = false
            if let error = error {
                self.error = ErrorHolder(error: error)
            } else {
                self.session = nil
                // Otherwise the next launch would sign the same user in again.
                UserDefaultsManager.shared.token = ""
                UserDefaultsManager.shared.secret = ""
            }
        }
    }

}
