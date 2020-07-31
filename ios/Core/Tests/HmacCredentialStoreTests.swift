import XCTest
@testable import FjuulCore

final class HmacCredentialStoreTests: XCTestCase {

    let key = SigningKey(
        id: "28c433cd-b2ec-4701-b4ab-269ac74c06cf",
        secret: "cabbf8b9-d16f-4783-978a-05d6940bd17b",
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
        XCTAssertEqual(persistor.get(key: "signing-key.token"), key)
    }

}
