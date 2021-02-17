import Foundation
import Alamofire
import FjuulCore

extension DataResponse {
    /// Helper for map FjuulError with Alomifire response
    /// - Parameter completionHandler: completionHandler
    /// - Returns: FjuulError or original error
    func mapActivitySourcesAPIError(completionHandler: (_ response: Self, _ errorJSONBody: ErrorJSONBodyResponse?) -> FjuulError.ActivitySourceConnectionFailureReason?) -> DataResponse<Success, Error> {

        return self.mapError { err -> Error in
            guard let responseData = self.data else { return err }
            guard let errorResponse = try? Decoders.iso8601Full.decode(ErrorJSONBodyResponse.self, from: responseData) else { return err }
            guard let reason = completionHandler(self, errorResponse) else {
                return err
            }

            return FjuulError.activitySourceConnectionFailure(reason: reason)
        }
    }
}
