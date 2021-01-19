import Foundation
import XCTest

@testable import FjuulCore

class TestStore {
    let lookupKey: String
    let persistor: Persistor

    init(userToken: String, persistor: Persistor) {
        self.persistor = persistor
        self.lookupKey = "test-key.\(userToken)"
    }

    var value: String? {
        get {
            return persistor.get(key: lookupKey)
        }
        set {
            persistor.set(key: lookupKey, value: newValue)
        }
    }
}

final class DiskPersistorTests: XCTestCase {
    var sut: TestStore!

    let userToken = "afcc3d53-f6e8-4094-a9c0-6bd6e1125a69"

    override func setUp() {
        sut = TestStore(userToken: userToken, persistor: DiskPersistor())

        super.setUp()
    }

    override func tearDown() {
        super.tearDown()
        
//        let tearDownResult = sut.persistor.remove(matchKey: "fjuul.sdk.persistence")
//
//        print("tearDownResult: ", tearDownResult)
    }
    
    func testClassFuncRemove() {
        // Given
        let anotherToken = "xxxxxxxxxxx"
        let anotherStore = TestStore(userToken: anotherToken, persistor: DiskPersistor())
        
        sut.value = "test value 1"
        anotherStore.value = "test value 2"
        
        let storeFolderURL = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        do {
            let fileURLs = try FileManager.default.contentsOfDirectory(at: storeFolderURL, includingPropertiesForKeys: nil)
            
            // Check that store files were created
            XCTAssert(fileURLs.contains { url in url.path.contains(userToken) })
            XCTAssert(fileURLs.contains { url in url.path.contains(anotherToken) })
        } catch {
            XCTFail("Error: on read files")
        }
        
        // When
        let removeResult = DiskPersistor.remove(matchKey: anotherToken)
        
        // Then
        XCTAssert(removeResult)
        do {
            let fileURLs = try FileManager.default.contentsOfDirectory(at: storeFolderURL, includingPropertiesForKeys: nil)
            
            // Not removed store should exists
            XCTAssert(fileURLs.contains { url in url.path.contains(userToken) })
            // Сhecking that the desired store has been deleted
            XCTAssertFalse(fileURLs.contains { url in url.path.contains(anotherToken) })
        } catch {
            XCTFail("Error: on read files")
        }
    }
    
    func testSetAndGet() {
        // Given
        let storageFolderURL = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        let fileUrl = storageFolderURL.appendingPathComponent("com.fjuul.sdk.persistence.\(sut.lookupKey)")
        let value = "stored value"

        // When
        sut.value = value
        
        // Then
        XCTAssert(FileManager.default.fileExists(atPath: fileUrl.path))
        XCTAssertEqual(sut.value, value)
    }
    
    func testRemove() {
        // Given
        let storageFolderURL = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        let fileUrl = storageFolderURL.appendingPathComponent("com.fjuul.sdk.persistence.\(sut.lookupKey)")
        let value = "stored value"

        sut.value = value
        
        XCTAssert(FileManager.default.fileExists(atPath: fileUrl.path))
        
        // When
        let removeResult = sut.persistor.remove(key: sut.lookupKey)
        
        // Then
        XCTAssert(removeResult)
        XCTAssertFalse(FileManager.default.fileExists(atPath: fileUrl.path))
    }
}
