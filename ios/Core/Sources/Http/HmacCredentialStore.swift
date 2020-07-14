import Foundation
import Alamofire

struct HmacCredentialStore {

    private let lookupKey: String
    private var persistor: Persistor
    private var backingSigningKey: SigningKey?

    init(userToken: String, persistor: Persistor) {
        self.persistor = persistor
        self.lookupKey = "signing-key.\(userToken)"
        self.backingSigningKey = persistor.get(key: lookupKey) as? SigningKey
    }

    var signingKey: SigningKey? {
        get {
            return backingSigningKey
        }
        set {
            backingSigningKey = newValue
            guard let unwrapped = newValue else {
                // TODO do we need to support unset in the Persistor protocol?
                return
            }
            persistor.set(key: lookupKey, value: unwrapped)
        }
    }

}
