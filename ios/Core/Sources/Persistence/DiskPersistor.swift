import Foundation

struct DiskPersistor: Persistor {

    func set(key: String, value: Codable) {
        do {
            let data = NSKeyedArchiver.archivedData(withRootObject: value)
            try data.write(to: getFullPathForKey(key), options: .atomic)
        } catch {
            print("Couldn't write file")
        }
    }

    func get(key: String) -> Any? {
        let path = getFullPathForKey(key)
        return NSKeyedUnarchiver.unarchiveObject(withFile: path.absoluteString)
    }

    private func getStorageDirectory() -> URL {
        let paths = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)
        return paths[0]
    }

    private func getFullPathForKey(_ key: String) -> URL {
        getStorageDirectory().appendingPathComponent("com.fjuul.sdk.persistence.\(key)")
    }

}
