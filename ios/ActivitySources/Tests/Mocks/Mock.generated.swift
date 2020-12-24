// Generated using Sourcery 1.0.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT



// Generated with SwiftyMocky 4.0.1

import SwiftyMocky
import XCTest
import FjuulActivitySources
import FjuulCore
@testable import FjuulActivitySources


// MARK: - ActivitySourcesApiClient

open class ActivitySourcesApiClientMock: ActivitySourcesApiClient, Mock {
    public init(sequencing sequencingPolicy: SequencingPolicy = .lastWrittenResolvedFirst, stubbing stubbingPolicy: StubbingPolicy = .wrap, file: StaticString = #file, line: UInt = #line) {
        SwiftyMockyTestObserver.setup()
        self.sequencingPolicy = sequencingPolicy
        self.stubbingPolicy = stubbingPolicy
        self.file = file
        self.line = line
    }

    var matcher: Matcher = Matcher.default
    var stubbingPolicy: StubbingPolicy = .wrap
    var sequencingPolicy: SequencingPolicy = .lastWrittenResolvedFirst
    private var invocations: [MethodType] = []
    private var methodReturnValues: [Given] = []
    private var methodPerformValues: [Perform] = []
    private var file: StaticString?
    private var line: UInt?

    public typealias PropertyStub = Given
    public typealias MethodStub = Given
    public typealias SubscriptStub = Given

    /// Convenience method - call setupMock() to extend debug information when failure occurs
    public func setupMock(file: StaticString = #file, line: UInt = #line) {
        self.file = file
        self.line = line
    }

    /// Clear mock internals. You can specify what to reset (invocations aka verify, givens or performs) or leave it empty to clear all mock internals
    public func resetMock(_ scopes: MockScope...) {
        let scopes: [MockScope] = scopes.isEmpty ? [.invocation, .given, .perform] : scopes
        if scopes.contains(.invocation) { invocations = [] }
        if scopes.contains(.given) { methodReturnValues = [] }
        if scopes.contains(.perform) { methodPerformValues = [] }
    }

    public var apiClient: ApiClient {
		get {	invocations.append(.p_apiClient_get); return __p_apiClient ?? givenGetterValue(.p_apiClient_get, "ActivitySourcesApiClientMock - stub value for apiClient was not defined") }
	}
	private var __p_apiClient: (ApiClient)?





    open func connect(activitySourceItem: ActivitySourcesItem, completion: @escaping (Result<ConnectionResult, Error>) -> Void) {
        addInvocation(.m_connect__activitySourceItem_activitySourceItemcompletion_completion(Parameter<ActivitySourcesItem>.value(`activitySourceItem`), Parameter<(Result<ConnectionResult, Error>) -> Void>.value(`completion`)))
		let perform = methodPerformValue(.m_connect__activitySourceItem_activitySourceItemcompletion_completion(Parameter<ActivitySourcesItem>.value(`activitySourceItem`), Parameter<(Result<ConnectionResult, Error>) -> Void>.value(`completion`))) as? (ActivitySourcesItem, @escaping (Result<ConnectionResult, Error>) -> Void) -> Void
		perform?(`activitySourceItem`, `completion`)
    }

    open func disconnect(activitySourceConnection: ActivitySourceConnection, completion: @escaping (Result<Void, Error>) -> Void) {
        addInvocation(.m_disconnect__activitySourceConnection_activitySourceConnectioncompletion_completion(Parameter<ActivitySourceConnection>.value(`activitySourceConnection`), Parameter<(Result<Void, Error>) -> Void>.value(`completion`)))
		let perform = methodPerformValue(.m_disconnect__activitySourceConnection_activitySourceConnectioncompletion_completion(Parameter<ActivitySourceConnection>.value(`activitySourceConnection`), Parameter<(Result<Void, Error>) -> Void>.value(`completion`))) as? (ActivitySourceConnection, @escaping (Result<Void, Error>) -> Void) -> Void
		perform?(`activitySourceConnection`, `completion`)
    }

    open func getCurrentConnections(completion: @escaping (Result<[TrackerConnection], Error>) -> Void) {
        addInvocation(.m_getCurrentConnections__completion_completion(Parameter<(Result<[TrackerConnection], Error>) -> Void>.value(`completion`)))
		let perform = methodPerformValue(.m_getCurrentConnections__completion_completion(Parameter<(Result<[TrackerConnection], Error>) -> Void>.value(`completion`))) as? (@escaping (Result<[TrackerConnection], Error>) -> Void) -> Void
		perform?(`completion`)
    }


    fileprivate enum MethodType {
        case m_connect__activitySourceItem_activitySourceItemcompletion_completion(Parameter<ActivitySourcesItem>, Parameter<(Result<ConnectionResult, Error>) -> Void>)
        case m_disconnect__activitySourceConnection_activitySourceConnectioncompletion_completion(Parameter<ActivitySourceConnection>, Parameter<(Result<Void, Error>) -> Void>)
        case m_getCurrentConnections__completion_completion(Parameter<(Result<[TrackerConnection], Error>) -> Void>)
        case p_apiClient_get

        static func compareParameters(lhs: MethodType, rhs: MethodType, matcher: Matcher) -> Matcher.ComparisonResult {
            switch (lhs, rhs) {
            case (.m_connect__activitySourceItem_activitySourceItemcompletion_completion(let lhsActivitysourceitem, let lhsCompletion), .m_connect__activitySourceItem_activitySourceItemcompletion_completion(let rhsActivitysourceitem, let rhsCompletion)):
				var results: [Matcher.ParameterComparisonResult] = []
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsActivitysourceitem, rhs: rhsActivitysourceitem, with: matcher), lhsActivitysourceitem, rhsActivitysourceitem, "activitySourceItem"))
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsCompletion, rhs: rhsCompletion, with: matcher), lhsCompletion, rhsCompletion, "completion"))
				return Matcher.ComparisonResult(results)

            case (.m_disconnect__activitySourceConnection_activitySourceConnectioncompletion_completion(let lhsActivitysourceconnection, let lhsCompletion), .m_disconnect__activitySourceConnection_activitySourceConnectioncompletion_completion(let rhsActivitysourceconnection, let rhsCompletion)):
				var results: [Matcher.ParameterComparisonResult] = []
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsActivitysourceconnection, rhs: rhsActivitysourceconnection, with: matcher), lhsActivitysourceconnection, rhsActivitysourceconnection, "activitySourceConnection"))
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsCompletion, rhs: rhsCompletion, with: matcher), lhsCompletion, rhsCompletion, "completion"))
				return Matcher.ComparisonResult(results)

            case (.m_getCurrentConnections__completion_completion(let lhsCompletion), .m_getCurrentConnections__completion_completion(let rhsCompletion)):
				var results: [Matcher.ParameterComparisonResult] = []
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsCompletion, rhs: rhsCompletion, with: matcher), lhsCompletion, rhsCompletion, "completion"))
				return Matcher.ComparisonResult(results)
            case (.p_apiClient_get,.p_apiClient_get): return Matcher.ComparisonResult.match
            default: return .none
            }
        }

        func intValue() -> Int {
            switch self {
            case let .m_connect__activitySourceItem_activitySourceItemcompletion_completion(p0, p1): return p0.intValue + p1.intValue
            case let .m_disconnect__activitySourceConnection_activitySourceConnectioncompletion_completion(p0, p1): return p0.intValue + p1.intValue
            case let .m_getCurrentConnections__completion_completion(p0): return p0.intValue
            case .p_apiClient_get: return 0
            }
        }
        func assertionName() -> String {
            switch self {
            case .m_connect__activitySourceItem_activitySourceItemcompletion_completion: return ".connect(activitySourceItem:completion:)"
            case .m_disconnect__activitySourceConnection_activitySourceConnectioncompletion_completion: return ".disconnect(activitySourceConnection:completion:)"
            case .m_getCurrentConnections__completion_completion: return ".getCurrentConnections(completion:)"
            case .p_apiClient_get: return "[get] .apiClient"
            }
        }
    }

    open class Given: StubbedMethod {
        fileprivate var method: MethodType

        private init(method: MethodType, products: [StubProduct]) {
            self.method = method
            super.init(products)
        }

        public static func apiClient(getter defaultValue: ApiClient...) -> PropertyStub {
            return Given(method: .p_apiClient_get, products: defaultValue.map({ StubProduct.return($0 as Any) }))
        }

    }

    public struct Verify {
        fileprivate var method: MethodType

        public static func connect(activitySourceItem: Parameter<ActivitySourcesItem>, completion: Parameter<(Result<ConnectionResult, Error>) -> Void>) -> Verify { return Verify(method: .m_connect__activitySourceItem_activitySourceItemcompletion_completion(`activitySourceItem`, `completion`))}
        public static func disconnect(activitySourceConnection: Parameter<ActivitySourceConnection>, completion: Parameter<(Result<Void, Error>) -> Void>) -> Verify { return Verify(method: .m_disconnect__activitySourceConnection_activitySourceConnectioncompletion_completion(`activitySourceConnection`, `completion`))}
        public static func getCurrentConnections(completion: Parameter<(Result<[TrackerConnection], Error>) -> Void>) -> Verify { return Verify(method: .m_getCurrentConnections__completion_completion(`completion`))}
        public static var apiClient: Verify { return Verify(method: .p_apiClient_get) }
    }

    public struct Perform {
        fileprivate var method: MethodType
        var performs: Any

        public static func connect(activitySourceItem: Parameter<ActivitySourcesItem>, completion: Parameter<(Result<ConnectionResult, Error>) -> Void>, perform: @escaping (ActivitySourcesItem, @escaping (Result<ConnectionResult, Error>) -> Void) -> Void) -> Perform {
            return Perform(method: .m_connect__activitySourceItem_activitySourceItemcompletion_completion(`activitySourceItem`, `completion`), performs: perform)
        }
        public static func disconnect(activitySourceConnection: Parameter<ActivitySourceConnection>, completion: Parameter<(Result<Void, Error>) -> Void>, perform: @escaping (ActivitySourceConnection, @escaping (Result<Void, Error>) -> Void) -> Void) -> Perform {
            return Perform(method: .m_disconnect__activitySourceConnection_activitySourceConnectioncompletion_completion(`activitySourceConnection`, `completion`), performs: perform)
        }
        public static func getCurrentConnections(completion: Parameter<(Result<[TrackerConnection], Error>) -> Void>, perform: @escaping (@escaping (Result<[TrackerConnection], Error>) -> Void) -> Void) -> Perform {
            return Perform(method: .m_getCurrentConnections__completion_completion(`completion`), performs: perform)
        }
    }

    public func given(_ method: Given) {
        methodReturnValues.append(method)
    }

    public func perform(_ method: Perform) {
        methodPerformValues.append(method)
        methodPerformValues.sort { $0.method.intValue() < $1.method.intValue() }
    }

    public func verify(_ method: Verify, count: Count = Count.moreOrEqual(to: 1), file: StaticString = #file, line: UInt = #line) {
        let fullMatches = matchingCalls(method, file: file, line: line)
        let success = count.matches(fullMatches)
        let assertionName = method.method.assertionName()
        let feedback: String = {
            guard !success else { return "" }
            return Utils.closestCallsMessage(
                for: self.invocations.map { invocation in
                    matcher.set(file: file, line: line)
                    defer { matcher.clearFileAndLine() }
                    return MethodType.compareParameters(lhs: invocation, rhs: method.method, matcher: matcher)
                },
                name: assertionName
            )
        }()
        MockyAssert(success, "Expected: \(count) invocations of `\(assertionName)`, but was: \(fullMatches).\(feedback)", file: file, line: line)
    }

    private func addInvocation(_ call: MethodType) {
        invocations.append(call)
    }
    private func methodReturnValue(_ method: MethodType) throws -> StubProduct {
        matcher.set(file: self.file, line: self.line)
        defer { matcher.clearFileAndLine() }
        let candidates = sequencingPolicy.sorted(methodReturnValues, by: { $0.method.intValue() > $1.method.intValue() })
        let matched = candidates.first(where: { $0.isValid && MethodType.compareParameters(lhs: $0.method, rhs: method, matcher: matcher).isFullMatch })
        guard let product = matched?.getProduct(policy: self.stubbingPolicy) else { throw MockError.notStubed }
        return product
    }
    private func methodPerformValue(_ method: MethodType) -> Any? {
        matcher.set(file: self.file, line: self.line)
        defer { matcher.clearFileAndLine() }
        let matched = methodPerformValues.reversed().first { MethodType.compareParameters(lhs: $0.method, rhs: method, matcher: matcher).isFullMatch }
        return matched?.performs
    }
    private func matchingCalls(_ method: MethodType, file: StaticString?, line: UInt?) -> [MethodType] {
        matcher.set(file: file ?? self.file, line: line ?? self.line)
        defer { matcher.clearFileAndLine() }
        return invocations.filter { MethodType.compareParameters(lhs: $0, rhs: method, matcher: matcher).isFullMatch }
    }
    private func matchingCalls(_ method: Verify, file: StaticString?, line: UInt?) -> Int {
        return matchingCalls(method.method, file: file, line: line).count
    }
    private func givenGetterValue<T>(_ method: MethodType, _ message: String) -> T {
        do {
            return try methodReturnValue(method).casted()
        } catch {
            onFatalFailure(message)
            Failure(message)
        }
    }
    private func optionalGivenGetterValue<T>(_ method: MethodType, _ message: String) -> T? {
        do {
            return try methodReturnValue(method).casted()
        } catch {
            return nil
        }
    }
    private func onFatalFailure(_ message: String) {
        guard let file = self.file, let line = self.line else { return } // Let if fail if cannot handle gratefully
        SwiftyMockyTestObserver.handleFatalError(message: message, file: file, line: line)
    }
}

// MARK: - MountableActivitySourceHK

open class MountableActivitySourceHKMock: MountableActivitySourceHK, Mock {
    public init(sequencing sequencingPolicy: SequencingPolicy = .lastWrittenResolvedFirst, stubbing stubbingPolicy: StubbingPolicy = .wrap, file: StaticString = #file, line: UInt = #line) {
        SwiftyMockyTestObserver.setup()
        self.sequencingPolicy = sequencingPolicy
        self.stubbingPolicy = stubbingPolicy
        self.file = file
        self.line = line
    }

    var matcher: Matcher = Matcher.default
    var stubbingPolicy: StubbingPolicy = .wrap
    var sequencingPolicy: SequencingPolicy = .lastWrittenResolvedFirst
    private var invocations: [MethodType] = []
    private var methodReturnValues: [Given] = []
    private var methodPerformValues: [Perform] = []
    private var file: StaticString?
    private var line: UInt?

    public typealias PropertyStub = Given
    public typealias MethodStub = Given
    public typealias SubscriptStub = Given

    /// Convenience method - call setupMock() to extend debug information when failure occurs
    public func setupMock(file: StaticString = #file, line: UInt = #line) {
        self.file = file
        self.line = line
    }

    /// Clear mock internals. You can specify what to reset (invocations aka verify, givens or performs) or leave it empty to clear all mock internals
    public func resetMock(_ scopes: MockScope...) {
        let scopes: [MockScope] = scopes.isEmpty ? [.invocation, .given, .perform] : scopes
        if scopes.contains(.invocation) { invocations = [] }
        if scopes.contains(.given) { methodReturnValues = [] }
        if scopes.contains(.perform) { methodPerformValues = [] }
    }

    public var tracker: ActivitySourcesItem {
		get {	invocations.append(.p_tracker_get); return __p_tracker ?? givenGetterValue(.p_tracker_get, "MountableActivitySourceHKMock - stub value for tracker was not defined") }
	}
	private var __p_tracker: (ActivitySourcesItem)?

    public var apiClient: ActivitySourcesApiClient? {
		get {	invocations.append(.p_apiClient_get); return __p_apiClient ?? optionalGivenGetterValue(.p_apiClient_get, "MountableActivitySourceHKMock - stub value for apiClient was not defined") }
	}
	private var __p_apiClient: (ActivitySourcesApiClient)?





    open func requestAccess(config: ActivitySourceConfigBuilder, completion: @escaping (Result<Bool, Error>) -> Void) {
        addInvocation(.m_requestAccess__config_configcompletion_completion(Parameter<ActivitySourceConfigBuilder>.value(`config`), Parameter<(Result<Bool, Error>) -> Void>.value(`completion`)))
		let perform = methodPerformValue(.m_requestAccess__config_configcompletion_completion(Parameter<ActivitySourceConfigBuilder>.value(`config`), Parameter<(Result<Bool, Error>) -> Void>.value(`completion`))) as? (ActivitySourceConfigBuilder, @escaping (Result<Bool, Error>) -> Void) -> Void
		perform?(`config`, `completion`)
    }

    open func mount(apiClient: ActivitySourcesApiClient, config: ActivitySourceConfigBuilder, persistor: Persistor, completion: @escaping (Result<Bool, Error>) -> Void) {
        addInvocation(.m_mount__apiClient_apiClientconfig_configpersistor_persistorcompletion_completion(Parameter<ActivitySourcesApiClient>.value(`apiClient`), Parameter<ActivitySourceConfigBuilder>.value(`config`), Parameter<Persistor>.value(`persistor`), Parameter<(Result<Bool, Error>) -> Void>.value(`completion`)))
		let perform = methodPerformValue(.m_mount__apiClient_apiClientconfig_configpersistor_persistorcompletion_completion(Parameter<ActivitySourcesApiClient>.value(`apiClient`), Parameter<ActivitySourceConfigBuilder>.value(`config`), Parameter<Persistor>.value(`persistor`), Parameter<(Result<Bool, Error>) -> Void>.value(`completion`))) as? (ActivitySourcesApiClient, ActivitySourceConfigBuilder, Persistor, @escaping (Result<Bool, Error>) -> Void) -> Void
		perform?(`apiClient`, `config`, `persistor`, `completion`)
    }

    open func unmount(completion: @escaping (Result<Bool, Error>) -> Void) {
        addInvocation(.m_unmount__completion_completion(Parameter<(Result<Bool, Error>) -> Void>.value(`completion`)))
		let perform = methodPerformValue(.m_unmount__completion_completion(Parameter<(Result<Bool, Error>) -> Void>.value(`completion`))) as? (@escaping (Result<Bool, Error>) -> Void) -> Void
		perform?(`completion`)
    }


    fileprivate enum MethodType {
        case m_requestAccess__config_configcompletion_completion(Parameter<ActivitySourceConfigBuilder>, Parameter<(Result<Bool, Error>) -> Void>)
        case m_mount__apiClient_apiClientconfig_configpersistor_persistorcompletion_completion(Parameter<ActivitySourcesApiClient>, Parameter<ActivitySourceConfigBuilder>, Parameter<Persistor>, Parameter<(Result<Bool, Error>) -> Void>)
        case m_unmount__completion_completion(Parameter<(Result<Bool, Error>) -> Void>)
        case p_tracker_get
        case p_apiClient_get

        static func compareParameters(lhs: MethodType, rhs: MethodType, matcher: Matcher) -> Matcher.ComparisonResult {
            switch (lhs, rhs) {
            case (.m_requestAccess__config_configcompletion_completion(let lhsConfig, let lhsCompletion), .m_requestAccess__config_configcompletion_completion(let rhsConfig, let rhsCompletion)):
				var results: [Matcher.ParameterComparisonResult] = []
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsConfig, rhs: rhsConfig, with: matcher), lhsConfig, rhsConfig, "config"))
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsCompletion, rhs: rhsCompletion, with: matcher), lhsCompletion, rhsCompletion, "completion"))
				return Matcher.ComparisonResult(results)

            case (.m_mount__apiClient_apiClientconfig_configpersistor_persistorcompletion_completion(let lhsApiclient, let lhsConfig, let lhsPersistor, let lhsCompletion), .m_mount__apiClient_apiClientconfig_configpersistor_persistorcompletion_completion(let rhsApiclient, let rhsConfig, let rhsPersistor, let rhsCompletion)):
				var results: [Matcher.ParameterComparisonResult] = []
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsApiclient, rhs: rhsApiclient, with: matcher), lhsApiclient, rhsApiclient, "apiClient"))
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsConfig, rhs: rhsConfig, with: matcher), lhsConfig, rhsConfig, "config"))
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsPersistor, rhs: rhsPersistor, with: matcher), lhsPersistor, rhsPersistor, "persistor"))
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsCompletion, rhs: rhsCompletion, with: matcher), lhsCompletion, rhsCompletion, "completion"))
				return Matcher.ComparisonResult(results)

            case (.m_unmount__completion_completion(let lhsCompletion), .m_unmount__completion_completion(let rhsCompletion)):
				var results: [Matcher.ParameterComparisonResult] = []
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsCompletion, rhs: rhsCompletion, with: matcher), lhsCompletion, rhsCompletion, "completion"))
				return Matcher.ComparisonResult(results)
            case (.p_tracker_get,.p_tracker_get): return Matcher.ComparisonResult.match
            case (.p_apiClient_get,.p_apiClient_get): return Matcher.ComparisonResult.match
            default: return .none
            }
        }

        func intValue() -> Int {
            switch self {
            case let .m_requestAccess__config_configcompletion_completion(p0, p1): return p0.intValue + p1.intValue
            case let .m_mount__apiClient_apiClientconfig_configpersistor_persistorcompletion_completion(p0, p1, p2, p3): return p0.intValue + p1.intValue + p2.intValue + p3.intValue
            case let .m_unmount__completion_completion(p0): return p0.intValue
            case .p_tracker_get: return 0
            case .p_apiClient_get: return 0
            }
        }
        func assertionName() -> String {
            switch self {
            case .m_requestAccess__config_configcompletion_completion: return ".requestAccess(config:completion:)"
            case .m_mount__apiClient_apiClientconfig_configpersistor_persistorcompletion_completion: return ".mount(apiClient:config:persistor:completion:)"
            case .m_unmount__completion_completion: return ".unmount(completion:)"
            case .p_tracker_get: return "[get] .tracker"
            case .p_apiClient_get: return "[get] .apiClient"
            }
        }
    }

    open class Given: StubbedMethod {
        fileprivate var method: MethodType

        private init(method: MethodType, products: [StubProduct]) {
            self.method = method
            super.init(products)
        }

        public static func tracker(getter defaultValue: ActivitySourcesItem...) -> PropertyStub {
            return Given(method: .p_tracker_get, products: defaultValue.map({ StubProduct.return($0 as Any) }))
        }
        public static func apiClient(getter defaultValue: ActivitySourcesApiClient?...) -> PropertyStub {
            return Given(method: .p_apiClient_get, products: defaultValue.map({ StubProduct.return($0 as Any) }))
        }

    }

    public struct Verify {
        fileprivate var method: MethodType

        public static func requestAccess(config: Parameter<ActivitySourceConfigBuilder>, completion: Parameter<(Result<Bool, Error>) -> Void>) -> Verify { return Verify(method: .m_requestAccess__config_configcompletion_completion(`config`, `completion`))}
        public static func mount(apiClient: Parameter<ActivitySourcesApiClient>, config: Parameter<ActivitySourceConfigBuilder>, persistor: Parameter<Persistor>, completion: Parameter<(Result<Bool, Error>) -> Void>) -> Verify { return Verify(method: .m_mount__apiClient_apiClientconfig_configpersistor_persistorcompletion_completion(`apiClient`, `config`, `persistor`, `completion`))}
        public static func unmount(completion: Parameter<(Result<Bool, Error>) -> Void>) -> Verify { return Verify(method: .m_unmount__completion_completion(`completion`))}
        public static var tracker: Verify { return Verify(method: .p_tracker_get) }
        public static var apiClient: Verify { return Verify(method: .p_apiClient_get) }
    }

    public struct Perform {
        fileprivate var method: MethodType
        var performs: Any

        public static func requestAccess(config: Parameter<ActivitySourceConfigBuilder>, completion: Parameter<(Result<Bool, Error>) -> Void>, perform: @escaping (ActivitySourceConfigBuilder, @escaping (Result<Bool, Error>) -> Void) -> Void) -> Perform {
            return Perform(method: .m_requestAccess__config_configcompletion_completion(`config`, `completion`), performs: perform)
        }
        public static func mount(apiClient: Parameter<ActivitySourcesApiClient>, config: Parameter<ActivitySourceConfigBuilder>, persistor: Parameter<Persistor>, completion: Parameter<(Result<Bool, Error>) -> Void>, perform: @escaping (ActivitySourcesApiClient, ActivitySourceConfigBuilder, Persistor, @escaping (Result<Bool, Error>) -> Void) -> Void) -> Perform {
            return Perform(method: .m_mount__apiClient_apiClientconfig_configpersistor_persistorcompletion_completion(`apiClient`, `config`, `persistor`, `completion`), performs: perform)
        }
        public static func unmount(completion: Parameter<(Result<Bool, Error>) -> Void>, perform: @escaping (@escaping (Result<Bool, Error>) -> Void) -> Void) -> Perform {
            return Perform(method: .m_unmount__completion_completion(`completion`), performs: perform)
        }
    }

    public func given(_ method: Given) {
        methodReturnValues.append(method)
    }

    public func perform(_ method: Perform) {
        methodPerformValues.append(method)
        methodPerformValues.sort { $0.method.intValue() < $1.method.intValue() }
    }

    public func verify(_ method: Verify, count: Count = Count.moreOrEqual(to: 1), file: StaticString = #file, line: UInt = #line) {
        let fullMatches = matchingCalls(method, file: file, line: line)
        let success = count.matches(fullMatches)
        let assertionName = method.method.assertionName()
        let feedback: String = {
            guard !success else { return "" }
            return Utils.closestCallsMessage(
                for: self.invocations.map { invocation in
                    matcher.set(file: file, line: line)
                    defer { matcher.clearFileAndLine() }
                    return MethodType.compareParameters(lhs: invocation, rhs: method.method, matcher: matcher)
                },
                name: assertionName
            )
        }()
        MockyAssert(success, "Expected: \(count) invocations of `\(assertionName)`, but was: \(fullMatches).\(feedback)", file: file, line: line)
    }

    private func addInvocation(_ call: MethodType) {
        invocations.append(call)
    }
    private func methodReturnValue(_ method: MethodType) throws -> StubProduct {
        matcher.set(file: self.file, line: self.line)
        defer { matcher.clearFileAndLine() }
        let candidates = sequencingPolicy.sorted(methodReturnValues, by: { $0.method.intValue() > $1.method.intValue() })
        let matched = candidates.first(where: { $0.isValid && MethodType.compareParameters(lhs: $0.method, rhs: method, matcher: matcher).isFullMatch })
        guard let product = matched?.getProduct(policy: self.stubbingPolicy) else { throw MockError.notStubed }
        return product
    }
    private func methodPerformValue(_ method: MethodType) -> Any? {
        matcher.set(file: self.file, line: self.line)
        defer { matcher.clearFileAndLine() }
        let matched = methodPerformValues.reversed().first { MethodType.compareParameters(lhs: $0.method, rhs: method, matcher: matcher).isFullMatch }
        return matched?.performs
    }
    private func matchingCalls(_ method: MethodType, file: StaticString?, line: UInt?) -> [MethodType] {
        matcher.set(file: file ?? self.file, line: line ?? self.line)
        defer { matcher.clearFileAndLine() }
        return invocations.filter { MethodType.compareParameters(lhs: $0, rhs: method, matcher: matcher).isFullMatch }
    }
    private func matchingCalls(_ method: Verify, file: StaticString?, line: UInt?) -> Int {
        return matchingCalls(method.method, file: file, line: line).count
    }
    private func givenGetterValue<T>(_ method: MethodType, _ message: String) -> T {
        do {
            return try methodReturnValue(method).casted()
        } catch {
            onFatalFailure(message)
            Failure(message)
        }
    }
    private func optionalGivenGetterValue<T>(_ method: MethodType, _ message: String) -> T? {
        do {
            return try methodReturnValue(method).casted()
        } catch {
            return nil
        }
    }
    private func onFatalFailure(_ message: String) {
        guard let file = self.file, let line = self.line else { return } // Let if fail if cannot handle gratefully
        SwiftyMockyTestObserver.handleFatalError(message: message, file: file, line: line)
    }
}

