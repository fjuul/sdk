import Foundation
import Alamofire

class HmacCredentialStore {

    private let lookupKey: String
    private var persistor: Persistor

    init(userToken: String, persistor: Persistor) {
        self.persistor = persistor
        self.lookupKey = "signing-key.\(userToken)"
    }

    var signingKey: SigningKey? {
        get {
            return persistor.get(key: lookupKey)
        }
        set {
            persistor.set(key: lookupKey, value: newValue)
        }
    }

}
