import Foundation
import FjuulCore
import FjuulUser
import FjuulActivitySources

class UserProfileObservable: ObservableObject {

    private var originalProfile: UserProfile?

    @Published var error: ErrorHolder?

    @Published var token = ""
    @Published var birthDate = Date(timeIntervalSince1970: 0)
    @Published var height: Float = 170
    @Published var weight: Float = 80
    @Published var gender: Gender = .other
    @Published var timezone = ""
    @Published var locale = ""

    init(fetchOnInit: Bool = false) {
        if fetchOnInit {
            fetch()
        }
    }

    private func hydrateFromUserProfile(_ profile: UserProfile) {
        self.originalProfile = profile
        self.token = profile.token
        self.birthDate = profile.birthDate
        self.height = (profile.height as NSDecimalNumber).floatValue
        self.weight = (profile.weight as NSDecimalNumber).floatValue
        self.gender = profile.gender
        self.timezone = profile.timezone.identifier
        self.locale = profile.locale
    }

    private func getDirtyFields() -> PartialUserProfile {
        var result = PartialUserProfile()
        if self.birthDate != originalProfile?.birthDate {
            result[\UserProfile.birthDate] = self.birthDate
        }
        if self.height != (originalProfile?.height as NSDecimalNumber?)?.floatValue {
            result[\UserProfile.height] = Decimal(string: String(describing: self.height))
        }
        if self.weight != (originalProfile?.weight as NSDecimalNumber?)?.floatValue {
            result[\UserProfile.weight] = Decimal(string: String(describing: self.weight))
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
        let profileData = PartialUserProfile { profile in
            profile[\.birthDate] = self.birthDate
            profile[\.gender] = self.gender
            profile[\.height] = Decimal(string: String(describing: self.height))
            profile[\.weight] = Decimal(string: String(describing: self.weight))
            profile[\.timezone] = TimeZone(identifier: self.timezone)
            profile[\.locale] = self.locale
        }
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

    func markUserForDeletion() -> Bool {
        var userDeleted = false
        ApiClientHolder.default.apiClient?.user.markUserForDeletion { result in
            switch result {
            case .success:
                userDeleted = true
            case .failure(let err):
                self.error = ErrorHolder(error: err)
            }
        }
        return userDeleted
    }

    func logout() -> Bool {
        guard let result = ApiClientHolder.default.apiClient?.clearPersistentStorage(), result else { return false }

        ApiClientHolder.default.apiClient?.activitySourcesManager?.unmount { result in
            switch result {
            case .success:
                ApiClientHolder.default.apiClient = nil
            case .failure(let err):
                self.error = ErrorHolder(error: err)
            }
        }
        return true
    }

}
