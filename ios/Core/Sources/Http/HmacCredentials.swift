import Foundation
import Alamofire

// TODO this should probably be enhanced by some kind of persistent storage mechanism
// so signing keys can survive an app restart
struct HmacCredentials: AuthenticationCredential {

    let signingKey: SigningKey?

    var requiresRefresh: Bool {
        guard let key = signingKey else {
            return true
        }
        return key.requiresRefresh
    }

}
