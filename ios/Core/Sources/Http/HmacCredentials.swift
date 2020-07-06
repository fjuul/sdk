import Foundation
import Alamofire

struct HmacCredentials: AuthenticationCredential {

    let signingKey: SigningKey?

    var requiresRefresh: Bool {
        guard let key = signingKey else {
            return true
        }
        return key.requiresRefresh
    }

}
