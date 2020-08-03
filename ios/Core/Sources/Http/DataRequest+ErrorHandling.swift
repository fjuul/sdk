import Foundation
import Alamofire

extension DataRequest {

    @discardableResult
    public func apiResponse(queue: DispatchQueue = .main,
                            dataPreprocessor: DataPreprocessor = DataResponseSerializer.defaultDataPreprocessor,
                            emptyResponseCodes: Set<Int> = DataResponseSerializer.defaultEmptyResponseCodes,
                            emptyRequestMethods: Set<HTTPMethod> = DataResponseSerializer.defaultEmptyRequestMethods,
                            completionHandler: @escaping (DataResponse<Data, Error>) -> Void) -> Self {

        let serializer = DataResponseSerializer(
            dataPreprocessor: dataPreprocessor,
            emptyResponseCodes: emptyResponseCodes,
            emptyRequestMethods: emptyRequestMethods
        )
        return validate().response(queue: queue, responseSerializer: serializer) { response in
            let mappedCustomErrors = response.mapError { error -> Error in
                guard let errorCode = response.response?.headers.value(for: "x-authentication-error") else {
                    return error
                }
                guard let reason = FjuulError.AuthenticationFailureReason(rawValue: errorCode) else {
                    return error
                }
                return FjuulError.authenticationFailure(reason: reason)
            }
            completionHandler(mappedCustomErrors)
        }

    }

}
