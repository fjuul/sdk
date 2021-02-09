import Foundation

public class DiskPersistor: Persistor {
    static internal let PERSISTANCE_PREFIX = "com.fjuul.sdk.persistence"

    public init() {}

    public static func remove(matchKey: String) -> Bool {
        do {
            let storeFolderURL = DiskPersistor.getStorageDirectory()
            let fileURLs = try FileManager.default.contentsOfDirectory(at: storeFolderURL, includingPropertiesForKeys: nil)

            try fileURLs.forEach { fileURL in
                if fileURL.path.range(of: "(\(DiskPersistor.PERSISTANCE_PREFIX)).+(\(matchKey))", options: .regularExpression) != nil {
                    try FileManager.default.removeItem(at: fileURL)
                }
            }

            return true
        } catch {
            return false
        }
    }

    public func set<T: Encodable>(key: String, value: T?) {
        do {
            // The Application Support directory does not exist by default, thus we need to create it here explicitly.
            // This is a no-op if the directory already exists.
            try FileManager.default.createDirectory(at: DiskPersistor.getStorageDirectory(), withIntermediateDirectories: true, attributes: nil)
            let fullPath = DiskPersistor.getFullPathForKey(key)

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
            DataLogger.shared.error("Error while reading persisted object: \(error)")
        }
    }

    public func get<T: Decodable>(key: String) -> T? {
        do {
            let path = DiskPersistor.getFullPathForKey(key)
            if !(FileManager.default.fileExists(atPath: path.path)) {
                return nil
            }
            let data = try Data(contentsOf: path)
            let unarchiver = NSKeyedUnarchiver(forReadingWith: data)
            return try unarchiver.decodeTopLevelDecodable(T.self, forKey: NSKeyedArchiveRootObjectKey)
        } catch {
            DataLogger.shared.error("Error while reading persisted object: \(error)")
            return nil
        }
    }

    public func remove(key: String) -> Bool {
        let fileURL = DiskPersistor.getFullPathForKey(key)

        do {
            try FileManager.default.removeItem(at: fileURL)
            return true
        } catch {
            return false
        }
    }

    private static func getStorageDirectory() -> URL {
        let paths = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)
        return paths[0]
    }

    private static func getFullPathForKey(_ key: String) -> URL {
        getStorageDirectory().appendingPathComponent("\(DiskPersistor.PERSISTANCE_PREFIX).\(key)")
    }

}
