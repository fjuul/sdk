import XCTest
@testable import FjuulCore

final class HmacCredentialStoreTests: XCTestCase {

    let key = SigningKey(
        id: "",
        secret: "",
        expiresAt: Date.distantFuture
    )

    func testInitWithoutKeyInStore() {
        let persistor = InMemoryPersistor()
        let credentialStore = HmacCredentialStore(userToken: "token", persistor: persistor)
        XCTAssertNil(credentialStore.signingKey)
    }

    func testInitWithExistingKeyInStore() {
        let persistor = InMemoryPersistor()
        persistor.set(key: "signing-key.token", value: key)
        let credentialStore = HmacCredentialStore(userToken: "token", persistor: persistor)
        XCTAssertEqual(credentialStore.signingKey, key)
    }

    func testPersistsOnSet() {
        let persistor = InMemoryPersistor()
        let credentialStore = HmacCredentialStore(userToken: "token", persistor: persistor)
        credentialStore.signingKey = key
        XCTAssertEqual(persistor.get(key: "signing-key.token") as? SigningKey, key)
    }

}
