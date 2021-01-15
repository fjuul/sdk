import Foundation

public class DiskPersistor: Persistor {
    public init() {}

    public func set<T: Encodable>(key: String, value: T?) {
        do {
            // The Application Support directory does not exist by default, thus we need to create it here explicitly.
            // This is a no-op if the directory already exists.
            try FileManager.default.createDirectory(at: getStorageDirectory(), withIntermediateDirectories: true, attributes: nil)
            let fullPath = getFullPathForKey(key)
            guard let unwrapped = value else {
                if FileManager.default.fileExists(atPath: fullPath.path) {
                    try FileManager.default.removeItem(atPath: fullPath.path)
                }
                return
            }
            let mutableData = NSMutableData()
            let archiver = NSKeyedArchiver(forWritingWith: mutableData)
            try archiver.encodeEncodable(unwrapped, forKey: NSKeyedArchiveRootObjectKey)
            archiver.finishEncoding()
            try mutableData.write(to: fullPath, options: .atomic)
        } catch {
            print("error while persisting object: \(error)")
        }
    }

    public func get<T: Decodable>(key: String) -> T? {
        do {
            let path = getFullPathForKey(key)
            if !(FileManager.default.fileExists(atPath: path.path)) {
                return nil
            }
            let data = try Data(contentsOf: path)
            let unarchiver = NSKeyedUnarchiver(forReadingWith: data)
            return try unarchiver.decodeTopLevelDecodable(T.self, forKey: NSKeyedArchiveRootObjectKey)
        } catch {
            print("error while reading persisted object: \(error)")
            return nil
        }
    }
    
    public func remove(matchKey: String) -> Bool {
        do {
            let storeFolder = getFullPathForKey("").path
            let paths = try FileManager.default.contentsOfDirectory(atPath: storeFolder)
            
            let fileteredPaths = paths.filter { patn in patn.contains(".\(matchKey)") }
            
            try fileteredPaths.forEach { filePath in
                try FileManager.default.removeItem(atPath: filePath)
            }
        } catch {}
        
        return false
    }

    private func getStorageDirectory() -> URL {
        let paths = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)
        return paths[0]
    }

    private func getFullPathForKey(_ key: String) -> URL {
        getStorageDirectory().appendingPathComponent("com.fjuul.sdk.persistence.\(key)")
    }

}
