import Foundation

struct DiskPersistor: Persistor {

    func set(key: String, value: Codable?) {
        do {
            let fullPath = getFullPathForKey(key)
            guard let unwrapped = value else {
                if FileManager.default.fileExists(atPath: fullPath.absoluteString) {
                    try FileManager.default.removeItem(atPath: fullPath.absoluteString)
                }
                return
            }
            let data = NSKeyedArchiver.archivedData(withRootObject: unwrapped)
            try data.write(to: fullPath, options: .atomic)
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
