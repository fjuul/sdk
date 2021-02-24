import Foundation
import Alamofire

extension DataResponse {
    /// Helper for map FjuulError with Alomifire response
    /// - Parameter completionHandler: completionHandler
    /// - Returns: FjuulError or original error
    public func mapAPIError(completionHandler: (_ response: Self, _ errorJSONBody: ErrorJSONBodyResponse?) -> FjuulError?) -> DataResponse<Success, Error> {

        return self.mapError { err -> Error in
            guard let responseData = self.data else { return err }
            guard let errorResponse = try? Decoders.iso8601Full.decode(ErrorJSONBodyResponse.self, from: responseData) else { return err }
            guard let fjuulError = completionHandler(self, errorResponse) else {
                return err
            }

            return fjuulError
        }
    }
}
