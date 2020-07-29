import Foundation
import FjuulCore
import FjuulUser

class UserProfileObservable: ObservableObject {

    private var originalProfile: UserProfile?

    @Published var error: ErrorHolder?

    @Published var token = ""
    @Published var birthDate = Date(timeIntervalSince1970: 0)
    @Published var height = 170
    @Published var weight = 80
    @Published var gender: Gender = .other
    @Published var timezone = ""
    @Published var locale: String?

    init(fetchOnInit: Bool = false) {
        if fetchOnInit {
            fetch()
        }
    }

    private func hydrateFromUserProfile(_ profile: UserProfile) {
        self.originalProfile = profile
        self.token = profile.token
        self.birthDate = profile.birthDate
        self.height = profile.height
        self.weight = profile.weight
        self.gender = profile.gender
        self.timezone = profile.timezone.identifier
        self.locale = profile.locale
    }

    private func getDirtyFields() -> PartialUserProfile {
        var result = PartialUserProfile()
        if self.birthDate != originalProfile?.birthDate {
            result[\UserProfile.birthDate] = self.birthDate
        }
        if self.height != originalProfile?.height {
            result[\UserProfile.height] = self.height
        }
        if self.weight != originalProfile?.weight {
            result[\UserProfile.weight] = self.weight
        }
        if self.gender != originalProfile?.gender {
            result[\UserProfile.gender] = self.gender
        }
        if self.timezone != originalProfile?.timezone.identifier {
            result[\UserProfile.timezone] = TimeZone(identifier: self.timezone)
        }
        return result
    }

    func fetch() {
        ApiClientHolder.default.apiClient?.user.getProfile { result in
            switch result {
            case .success(let profile): self.hydrateFromUserProfile(profile)
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
        }
    }

    func createNewUser(baseUrl: String, apiKey: String, completion: @escaping (Result<UserCreationResult, Error>) -> Void) {
        let profileData = PartialUserProfile([
            \UserProfile.birthDate: self.birthDate,
            \UserProfile.gender: self.gender,
            \UserProfile.height: self.height,
            \UserProfile.weight: self.weight,
            \UserProfile.timezone: self.timezone,
            \UserProfile.locale: self.locale,
        ])
        return ApiClient.createUser(baseUrl: baseUrl, apiKey: apiKey, profile: profileData) { result in
            switch result {
            case .success(let creationResult): self.hydrateFromUserProfile(creationResult.user)
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
            completion(result)
        }
    }

    func updateUser(completion: @escaping (Result<UserProfile, Error>) -> Void) {
        ApiClientHolder.default.apiClient?.user.updateProfile(self.getDirtyFields()) { result in
            switch result {
            case .success(let profile): self.hydrateFromUserProfile(profile)
            case .failure(let err): self.error = ErrorHolder(error: err)
            }
            completion(result)
        }
    }

}
