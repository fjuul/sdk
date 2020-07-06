import Foundation
import Alamofire

struct HmacCredentials: AuthenticationCredential {

    let userCredentials: UserCredentials
    let signingKey: SigningKey?

    var requiresRefresh: Bool {
        guard let key = signingKey else {
            return true
        }
        return key.requiresRefresh
    }

}
