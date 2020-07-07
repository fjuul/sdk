import Foundation
import Alamofire
import CommonCrypto

extension URLRequest {

    mutating func signWith(key: SigningKey, forDate date: Date) {

        var signingString = ""

        let signedHeaders = self.isRequestWithDigestChecking()
            ? "(request-target) date digest"
            : "(request-target) date"

        let httpMethod = self.httpMethod?.lowercased() ?? ""
        let path = self.url?.path ?? ""
        signingString.append("(request-target): \(httpMethod) \(path)\n")

        let formattedDate = DateFormatter.rfc1123.string(from: date)
        self.headers.add(name: "Date", value: formattedDate)

        signingString.append("date: \(formattedDate)")

        if self.isRequestWithDigestChecking() {
            let bodyData = self.httpBody ?? Data()
            if bodyData.isEmpty {
                signingString.append("\ndigest: ")
                self.headers.add(name: "Digest", value: "")
            } else {
                let bodyDigest = bodyData.sha256()
                let headerValue = "SHA-256=\(bodyDigest)"
                signingString.append("\ndigest: \(headerValue)")
                self.headers.add(name: "Digest", value: headerValue)
            }
        }

        let signature = signingString.hmac(key: key.secret)
        let signatureHeader = "keyId=\"\(key.id)\",algorithm=\"hmac-sha256\",headers=\"\(signedHeaders)\",signature=\"\(signature)\""
        self.headers.add(name: "Signature", value: signatureHeader)

    }

    private func isRequestWithDigestChecking() -> Bool {
        let methodsWithDigestChecking = [HTTPMethod.put, HTTPMethod.post]
        guard let method = self.method else {
            return false
        }
        return methodsWithDigestChecking.contains(method)
    }

}

extension Data {

    fileprivate func sha256() -> String {
        var hash = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
        self.withUnsafeBytes {
            _ = CC_SHA256($0.baseAddress, CC_LONG(self.count), &hash)
        }
        return Data(hash).base64EncodedString()
    }

}

extension String {

    fileprivate func hmac(key: String) -> String {
        var digest = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
        CCHmac(CCHmacAlgorithm(kCCHmacAlgSHA256), key, key.count, self, self.count, &digest)
        let data = Data(digest)
        return data.base64EncodedString()
    }

}

extension DateFormatter {

    static let rfc1123: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "EEE, dd MMM yyyy HH:mm:ss z"
        return formatter
    }()

}
