import Foundation
import Alamofire

struct HmacCredentialsStore: AuthenticationCredential {

    private var persistor: Persistor
    private var backingKey: SigningKey?

    var signingKey: SigningKey? {
        get {
            return backingKey
        }
        set(newKey) {
            backingKey = newKey
            persistor.set(key: "", value: newKey)
        }
    }

    init(userToken: String, persistor: Persistor) {
        self.persistor = persistor
        self.backingKey = persistor.get(key: "signing-key.\(userToken)") as? SigningKey
    }
    
    var requiresRefresh: Bool {
        guard let key = signingKey else {
            return true
        }
        return key.requiresRefresh
    }

}
