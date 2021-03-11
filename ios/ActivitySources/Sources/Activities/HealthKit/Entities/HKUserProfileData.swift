import Foundation

struct HKUserProfileData: Codable {
    var height: Double? // Unit cm
    var weight: Double? // Unit kg
}

extension HKUserProfileData {
    func asJsonEncodableDictionary() -> [String: Any] {
        var dict: [String: Any] = [:]

        if let height = self.height {
            dict["height"] = height
        }

        if let weight = self.weight {
            dict["weight"] = weight
        }

        return dict
    }
}
