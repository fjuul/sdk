import XCTest
@testable import FjuulCore

struct TestStruct: PartiallyEncodable {

    static func key(for keyPath: PartialKeyPath<TestStruct>) -> String? {
        switch keyPath {
        case \TestStruct.stringProp: return "keyA"
        case \TestStruct.timezoneProp: return "keyB"
        default: return nil
        }
    }

    static func jsonEncodableValue(for key: PartialKeyPath<TestStruct>, in value: Partial<TestStruct>) -> Any {
        switch key {
        case \TestStruct.timezoneProp: return value[\.timezoneProp]!.identifier
        default: return value[key]!
        }
    }

    let stringProp: String
    let timezoneProp: TimeZone
    let ommittedProp: String

}

final class PartialTests: XCTestCase {

    func testOmitsMappedButUnsetPropsInSerialization() {
        let data = Partial<TestStruct>()
        let result = data.asJsonEncodableDictionary()
        XCTAssertEqual(result.count, 0)
    }

    func testOmitsUnmappedPropsInSerialization() {
        let data = Partial<TestStruct> { partial in
            partial[\.ommittedProp] = "Hello World"
        }
        let result = data.asJsonEncodableDictionary()
        XCTAssertEqual(result.count, 0)
    }

    func testConvertsNonJsonConvertibleProps() {
        let data = Partial<TestStruct> { partial in
            partial[\.timezoneProp] = TimeZone.init(identifier: "Europe/Berlin")
        }
        let result = data.asJsonEncodableDictionary()
        XCTAssertEqual(result["keyB"] as? String, "Europe/Berlin")
    }

    func testIncludesMappedPropsIfSetInSerialization() {
        let data = Partial<TestStruct> { profile in
            profile[\.stringProp] = "Hello World"
            profile[\.timezoneProp] = TimeZone.current
        }
        let result = data.asJsonEncodableDictionary()
        XCTAssertEqual(result["keyA"] as? String, "Hello World")
        XCTAssertEqual(result["keyB"] as? String, TimeZone.current.identifier)
    }

}
