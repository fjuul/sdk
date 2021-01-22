// Generated using Sourcery 1.0.0 — https://github.com/krzysztofzablocki/Sourcery
// DO NOT EDIT

// swiftlint:disable all

// Generated with SwiftyMocky 4.0.1

import SwiftyMocky
import XCTest
import FjuulActivitySources
import FjuulCore
import HealthKit
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
            case (.p_apiClient_get, .p_apiClient_get): return Matcher.ComparisonResult.match
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

// MARK: - HealthKitManagerBuildering

open class HealthKitManagerBuilderingMock: HealthKitManagerBuildering, Mock {
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

    public required init(apiClient: ActivitySourcesApiClient, persistor: Persistor, config: ActivitySourceConfigBuilder) { }

    open func create(dataHandler: @escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)) -> HealthKitManaging {
        addInvocation(.m_create__dataHandler_dataHandler(Parameter<(_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void>.value(`dataHandler`)))
		let perform = methodPerformValue(.m_create__dataHandler_dataHandler(Parameter<(_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void>.value(`dataHandler`))) as? (@escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)) -> Void
		perform?(`dataHandler`)
		var __value: HealthKitManaging
		do {
		    __value = try methodReturnValue(.m_create__dataHandler_dataHandler(Parameter<(_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void>.value(`dataHandler`))).casted()
		} catch {
			onFatalFailure("Stub return value not specified for create(dataHandler: @escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)). Use given")
			Failure("Stub return value not specified for create(dataHandler: @escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)). Use given")
		}
		return __value
    }

    fileprivate enum MethodType {
        case m_create__dataHandler_dataHandler(Parameter<(_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void>)

        static func compareParameters(lhs: MethodType, rhs: MethodType, matcher: Matcher) -> Matcher.ComparisonResult {
            switch (lhs, rhs) {
            case (.m_create__dataHandler_dataHandler(let lhsDatahandler), .m_create__dataHandler_dataHandler(let rhsDatahandler)):
				var results: [Matcher.ParameterComparisonResult] = []
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsDatahandler, rhs: rhsDatahandler, with: matcher), lhsDatahandler, rhsDatahandler, "dataHandler"))
				return Matcher.ComparisonResult(results)
            }
        }

        func intValue() -> Int {
            switch self {
            case let .m_create__dataHandler_dataHandler(p0): return p0.intValue
            }
        }
        func assertionName() -> String {
            switch self {
            case .m_create__dataHandler_dataHandler: return ".create(dataHandler:)"
            }
        }
    }

    open class Given: StubbedMethod {
        fileprivate var method: MethodType

        private init(method: MethodType, products: [StubProduct]) {
            self.method = method
            super.init(products)
        }


        public static func create(dataHandler: Parameter<(_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void>, willReturn: HealthKitManaging...) -> MethodStub {
            return Given(method: .m_create__dataHandler_dataHandler(`dataHandler`), products: willReturn.map({ StubProduct.return($0 as Any) }))
        }
        public static func create(dataHandler: Parameter<(_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void>, willProduce: (Stubber<HealthKitManaging>) -> Void) -> MethodStub {
            let willReturn: [HealthKitManaging] = []
			let given: Given = { return Given(method: .m_create__dataHandler_dataHandler(`dataHandler`), products: willReturn.map({ StubProduct.return($0 as Any) })) }()
			let stubber = given.stub(for: (HealthKitManaging).self)
			willProduce(stubber)
			return given
        }
    }

    public struct Verify {
        fileprivate var method: MethodType

        public static func create(dataHandler: Parameter<(_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void>) -> Verify { return Verify(method: .m_create__dataHandler_dataHandler(`dataHandler`))}
    }

    public struct Perform {
        fileprivate var method: MethodType
        var performs: Any

        public static func create(dataHandler: Parameter<(_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void>, perform: @escaping (@escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)) -> Void) -> Perform {
            return Perform(method: .m_create__dataHandler_dataHandler(`dataHandler`), performs: perform)
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

// MARK: - HealthKitManaging

open class HealthKitManagingMock: HealthKitManaging, Mock, StaticMock {
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
    static var matcher: Matcher = Matcher.default
    static var stubbingPolicy: StubbingPolicy = .wrap
    static var sequencingPolicy: SequencingPolicy = .lastWrittenResolvedFirst
    static private var invocations: [StaticMethodType] = []
    static private var methodReturnValues: [StaticGiven] = []
    static private var methodPerformValues: [StaticPerform] = []
    public typealias StaticPropertyStub = StaticGiven
    public typealias StaticMethodStub = StaticGiven

    /// Clear mock internals. You can specify what to reset (invocations aka verify, givens or performs) or leave it empty to clear all mock internals
    public static func resetMock(_ scopes: MockScope...) {
        let scopes: [MockScope] = scopes.isEmpty ? [.invocation, .given, .perform] : scopes
        if scopes.contains(.invocation) { invocations = [] }
        if scopes.contains(.given) { methodReturnValues = [] }
        if scopes.contains(.perform) { methodPerformValues = [] }
    }


    public static var healthStore: HKHealthStore {
		get {	HealthKitManagingMock.invocations.append(.p_healthStore_get); return HealthKitManagingMock.__p_healthStore ?? givenGetterValue(.p_healthStore_get, "HealthKitManagingMock - stub value for healthStore was not defined") }
	}
	private static var __p_healthStore: (HKHealthStore)?




    public static func requestAccess(config: ActivitySourceConfigBuilder, completion: @escaping (Result<Bool, Error>) -> Void) {
        addInvocation(.sm_requestAccess__config_configcompletion_completion(Parameter<ActivitySourceConfigBuilder>.value(`config`), Parameter<(Result<Bool, Error>) -> Void>.value(`completion`)))
		let perform = methodPerformValue(.sm_requestAccess__config_configcompletion_completion(Parameter<ActivitySourceConfigBuilder>.value(`config`), Parameter<(Result<Bool, Error>) -> Void>.value(`completion`))) as? (ActivitySourceConfigBuilder, @escaping (Result<Bool, Error>) -> Void) -> Void
		perform?(`config`, `completion`)
    }

    public required init(anchorStore: HKAnchorStore, config: ActivitySourceConfigBuilder, dataHandler: @escaping ((_ data: HKRequestData?, _ completion: @escaping (Result<Bool, Error>) -> Void) -> Void)) { }

    open func mount(completion: @escaping (Result<Bool, Error>) -> Void) {
        addInvocation(.m_mount__completion_completion(Parameter<(Result<Bool, Error>) -> Void>.value(`completion`)))
		let perform = methodPerformValue(.m_mount__completion_completion(Parameter<(Result<Bool, Error>) -> Void>.value(`completion`))) as? (@escaping (Result<Bool, Error>) -> Void) -> Void
		perform?(`completion`)
    }

    open func disableAllBackgroundDelivery(completion: @escaping (Result<Bool, Error>) -> Void) {
        addInvocation(.m_disableAllBackgroundDelivery__completion_completion(Parameter<(Result<Bool, Error>) -> Void>.value(`completion`)))
		let perform = methodPerformValue(.m_disableAllBackgroundDelivery__completion_completion(Parameter<(Result<Bool, Error>) -> Void>.value(`completion`))) as? (@escaping (Result<Bool, Error>) -> Void) -> Void
		perform?(`completion`)
    }

    open func sync(completion: @escaping (Result<Bool, Error>) -> Void) {
        addInvocation(.m_sync__completion_completion(Parameter<(Result<Bool, Error>) -> Void>.value(`completion`)))
		let perform = methodPerformValue(.m_sync__completion_completion(Parameter<(Result<Bool, Error>) -> Void>.value(`completion`))) as? (@escaping (Result<Bool, Error>) -> Void) -> Void
		perform?(`completion`)
    }

    fileprivate enum StaticMethodType {
        case sm_requestAccess__config_configcompletion_completion(Parameter<ActivitySourceConfigBuilder>, Parameter<(Result<Bool, Error>) -> Void>)
        case p_healthStore_get

        static func compareParameters(lhs: StaticMethodType, rhs: StaticMethodType, matcher: Matcher) -> Matcher.ComparisonResult {
            switch (lhs, rhs) {
            case (.sm_requestAccess__config_configcompletion_completion(let lhsConfig, let lhsCompletion), .sm_requestAccess__config_configcompletion_completion(let rhsConfig, let rhsCompletion)):
				var results: [Matcher.ParameterComparisonResult] = []
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsConfig, rhs: rhsConfig, with: matcher), lhsConfig, rhsConfig, "config"))
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsCompletion, rhs: rhsCompletion, with: matcher), lhsCompletion, rhsCompletion, "completion"))
				return Matcher.ComparisonResult(results)
            case (.p_healthStore_get, .p_healthStore_get): return Matcher.ComparisonResult.match
            default: return .none
            }
        }

        func intValue() -> Int {
            switch self {
            case let .sm_requestAccess__config_configcompletion_completion(p0, p1): return p0.intValue + p1.intValue
            case .p_healthStore_get: return 0
            }
        }
        func assertionName() -> String {
            switch self {
            case .sm_requestAccess__config_configcompletion_completion: return ".requestAccess(config:completion:)"
            case .p_healthStore_get: return "[get] .healthStore"

            }
        }
    }

    open class StaticGiven: StubbedMethod {
        fileprivate var method: StaticMethodType

        private init(method: StaticMethodType, products: [StubProduct]) {
            self.method = method
            super.init(products)
        }

        public static func healthStore(getter defaultValue: HKHealthStore...) -> StaticPropertyStub {
            return StaticGiven(method: .p_healthStore_get, products: defaultValue.map({ StubProduct.return($0 as Any) }))
        }

    }

    public struct StaticVerify {
        fileprivate var method: StaticMethodType

        public static func requestAccess(config: Parameter<ActivitySourceConfigBuilder>, completion: Parameter<(Result<Bool, Error>) -> Void>) -> StaticVerify { return StaticVerify(method: .sm_requestAccess__config_configcompletion_completion(`config`, `completion`))}
        public static var healthStore: StaticVerify { return StaticVerify(method: .p_healthStore_get) }
    }

    public struct StaticPerform {
        fileprivate var method: StaticMethodType
        var performs: Any

        public static func requestAccess(config: Parameter<ActivitySourceConfigBuilder>, completion: Parameter<(Result<Bool, Error>) -> Void>, perform: @escaping (ActivitySourceConfigBuilder, @escaping (Result<Bool, Error>) -> Void) -> Void) -> StaticPerform {
            return StaticPerform(method: .sm_requestAccess__config_configcompletion_completion(`config`, `completion`), performs: perform)
        }
    }

    
    fileprivate enum MethodType {
        case m_mount__completion_completion(Parameter<(Result<Bool, Error>) -> Void>)
        case m_disableAllBackgroundDelivery__completion_completion(Parameter<(Result<Bool, Error>) -> Void>)
        case m_sync__completion_completion(Parameter<(Result<Bool, Error>) -> Void>)

        static func compareParameters(lhs: MethodType, rhs: MethodType, matcher: Matcher) -> Matcher.ComparisonResult {
            switch (lhs, rhs) {
            case (.m_mount__completion_completion(let lhsCompletion), .m_mount__completion_completion(let rhsCompletion)):
				var results: [Matcher.ParameterComparisonResult] = []
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsCompletion, rhs: rhsCompletion, with: matcher), lhsCompletion, rhsCompletion, "completion"))
				return Matcher.ComparisonResult(results)

            case (.m_disableAllBackgroundDelivery__completion_completion(let lhsCompletion), .m_disableAllBackgroundDelivery__completion_completion(let rhsCompletion)):
				var results: [Matcher.ParameterComparisonResult] = []
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsCompletion, rhs: rhsCompletion, with: matcher), lhsCompletion, rhsCompletion, "completion"))
				return Matcher.ComparisonResult(results)

            case (.m_sync__completion_completion(let lhsCompletion), .m_sync__completion_completion(let rhsCompletion)):
				var results: [Matcher.ParameterComparisonResult] = []
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsCompletion, rhs: rhsCompletion, with: matcher), lhsCompletion, rhsCompletion, "completion"))
				return Matcher.ComparisonResult(results)
            default: return .none
            }
        }

        func intValue() -> Int {
            switch self {
            case let .m_mount__completion_completion(p0): return p0.intValue
            case let .m_disableAllBackgroundDelivery__completion_completion(p0): return p0.intValue
            case let .m_sync__completion_completion(p0): return p0.intValue
            }
        }
        func assertionName() -> String {
            switch self {
            case .m_mount__completion_completion: return ".mount(completion:)"
            case .m_disableAllBackgroundDelivery__completion_completion: return ".disableAllBackgroundDelivery(completion:)"
            case .m_sync__completion_completion: return ".sync(completion:)"
            }
        }
    }

    open class Given: StubbedMethod {
        fileprivate var method: MethodType

        private init(method: MethodType, products: [StubProduct]) {
            self.method = method
            super.init(products)
        }


    }

    public struct Verify {
        fileprivate var method: MethodType

        public static func mount(completion: Parameter<(Result<Bool, Error>) -> Void>) -> Verify { return Verify(method: .m_mount__completion_completion(`completion`))}
        public static func disableAllBackgroundDelivery(completion: Parameter<(Result<Bool, Error>) -> Void>) -> Verify { return Verify(method: .m_disableAllBackgroundDelivery__completion_completion(`completion`))}
        public static func sync(completion: Parameter<(Result<Bool, Error>) -> Void>) -> Verify { return Verify(method: .m_sync__completion_completion(`completion`))}
    }

    public struct Perform {
        fileprivate var method: MethodType
        var performs: Any

        public static func mount(completion: Parameter<(Result<Bool, Error>) -> Void>, perform: @escaping (@escaping (Result<Bool, Error>) -> Void) -> Void) -> Perform {
            return Perform(method: .m_mount__completion_completion(`completion`), performs: perform)
        }
        public static func disableAllBackgroundDelivery(completion: Parameter<(Result<Bool, Error>) -> Void>, perform: @escaping (@escaping (Result<Bool, Error>) -> Void) -> Void) -> Perform {
            return Perform(method: .m_disableAllBackgroundDelivery__completion_completion(`completion`), performs: perform)
        }
        public static func sync(completion: Parameter<(Result<Bool, Error>) -> Void>, perform: @escaping (@escaping (Result<Bool, Error>) -> Void) -> Void) -> Perform {
            return Perform(method: .m_sync__completion_completion(`completion`), performs: perform)
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

    static public func given(_ method: StaticGiven) {
        methodReturnValues.append(method)
    }

    static public func perform(_ method: StaticPerform) {
        methodPerformValues.append(method)
        methodPerformValues.sort { $0.method.intValue() < $1.method.intValue() }
    }

    static public func verify(_ method: StaticVerify, count: Count = Count.moreOrEqual(to: 1), file: StaticString = #file, line: UInt = #line) {
        let fullMatches = matchingCalls(method, file: file, line: line)
        let success = count.matches(fullMatches)
        let assertionName = method.method.assertionName()
        let feedback: String = {
            guard !success else { return "" }
            return Utils.closestCallsMessage(
                for: self.invocations.map { invocation in
                    matcher.set(file: file, line: line)
                    defer { matcher.clearFileAndLine() }
                    return StaticMethodType.compareParameters(lhs: invocation, rhs: method.method, matcher: matcher)
                },
                name: assertionName
            )
        }()
        MockyAssert(success, "Expected: \(count) invocations of `\(assertionName)`, but was: \(fullMatches).\(feedback)", file: file, line: line)
    }

    static private func addInvocation(_ call: StaticMethodType) {
        invocations.append(call)
    }
    static private func methodReturnValue(_ method: StaticMethodType) throws -> StubProduct {
        let candidates = sequencingPolicy.sorted(methodReturnValues, by: { $0.method.intValue() > $1.method.intValue() })
        let matched = candidates.first(where: { $0.isValid && StaticMethodType.compareParameters(lhs: $0.method, rhs: method, matcher: matcher).isFullMatch })
        guard let product = matched?.getProduct(policy: self.stubbingPolicy) else { throw MockError.notStubed }
        return product
    }
    static private func methodPerformValue(_ method: StaticMethodType) -> Any? {
        let matched = methodPerformValues.reversed().first { StaticMethodType.compareParameters(lhs: $0.method, rhs: method, matcher: matcher).isFullMatch }
        return matched?.performs
    }
    static private func matchingCalls(_ method: StaticMethodType, file: StaticString?, line: UInt?) -> [StaticMethodType] {
        matcher.set(file: file, line: line)
        defer { matcher.clearFileAndLine() }
        return invocations.filter { StaticMethodType.compareParameters(lhs: $0, rhs: method, matcher: matcher).isFullMatch }
    }
    static private func matchingCalls(_ method: StaticVerify, file: StaticString?, line: UInt?) -> Int {
        return matchingCalls(method.method, file: file, line: line).count
    }
    static private func givenGetterValue<T>(_ method: StaticMethodType, _ message: String) -> T {
        do {
            return try methodReturnValue(method).casted()
        } catch {
            Failure(message)
        }
    }
    static private func optionalGivenGetterValue<T>(_ method: StaticMethodType, _ message: String) -> T? {
        do {
            return try methodReturnValue(method).casted()
        } catch {
            return nil
        }
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

    open func mount(apiClient: ActivitySourcesApiClient, config: ActivitySourceConfigBuilder, healthKitManagerBuilder: HealthKitManagerBuildering, completion: @escaping (Result<Bool, Error>) -> Void) {
        addInvocation(.m_mount__apiClient_apiClientconfig_confighealthKitManagerBuilder_healthKitManagerBuildercompletion_completion(Parameter<ActivitySourcesApiClient>.value(`apiClient`), Parameter<ActivitySourceConfigBuilder>.value(`config`), Parameter<HealthKitManagerBuildering>.value(`healthKitManagerBuilder`), Parameter<(Result<Bool, Error>) -> Void>.value(`completion`)))
		let perform = methodPerformValue(.m_mount__apiClient_apiClientconfig_confighealthKitManagerBuilder_healthKitManagerBuildercompletion_completion(Parameter<ActivitySourcesApiClient>.value(`apiClient`), Parameter<ActivitySourceConfigBuilder>.value(`config`), Parameter<HealthKitManagerBuildering>.value(`healthKitManagerBuilder`), Parameter<(Result<Bool, Error>) -> Void>.value(`completion`))) as? (ActivitySourcesApiClient, ActivitySourceConfigBuilder, HealthKitManagerBuildering, @escaping (Result<Bool, Error>) -> Void) -> Void
		perform?(`apiClient`, `config`, `healthKitManagerBuilder`, `completion`)
    }

    open func unmount(completion: @escaping (Result<Bool, Error>) -> Void) {
        addInvocation(.m_unmount__completion_completion(Parameter<(Result<Bool, Error>) -> Void>.value(`completion`)))
		let perform = methodPerformValue(.m_unmount__completion_completion(Parameter<(Result<Bool, Error>) -> Void>.value(`completion`))) as? (@escaping (Result<Bool, Error>) -> Void) -> Void
		perform?(`completion`)
    }


    fileprivate enum MethodType {
        case m_requestAccess__config_configcompletion_completion(Parameter<ActivitySourceConfigBuilder>, Parameter<(Result<Bool, Error>) -> Void>)
        case m_mount__apiClient_apiClientconfig_confighealthKitManagerBuilder_healthKitManagerBuildercompletion_completion(Parameter<ActivitySourcesApiClient>, Parameter<ActivitySourceConfigBuilder>, Parameter<HealthKitManagerBuildering>, Parameter<(Result<Bool, Error>) -> Void>)
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

            case (.m_mount__apiClient_apiClientconfig_confighealthKitManagerBuilder_healthKitManagerBuildercompletion_completion(let lhsApiclient, let lhsConfig, let lhsHealthkitmanagerbuilder, let lhsCompletion), .m_mount__apiClient_apiClientconfig_confighealthKitManagerBuilder_healthKitManagerBuildercompletion_completion(let rhsApiclient, let rhsConfig, let rhsHealthkitmanagerbuilder, let rhsCompletion)):
				var results: [Matcher.ParameterComparisonResult] = []
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsApiclient, rhs: rhsApiclient, with: matcher), lhsApiclient, rhsApiclient, "apiClient"))
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsConfig, rhs: rhsConfig, with: matcher), lhsConfig, rhsConfig, "config"))
				results.append(Matcher.ParameterComparisonResult(Parameter.compare(lhs: lhsHealthkitmanagerbuilder, rhs: rhsHealthkitmanagerbuilder, with: matcher), lhsHealthkitmanagerbuilder, rhsHealthkitmanagerbuilder, "healthKitManagerBuilder"))
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
            case let .m_mount__apiClient_apiClientconfig_confighealthKitManagerBuilder_healthKitManagerBuildercompletion_completion(p0, p1, p2, p3): return p0.intValue + p1.intValue + p2.intValue + p3.intValue
            case let .m_unmount__completion_completion(p0): return p0.intValue
            case .p_tracker_get: return 0
            case .p_apiClient_get: return 0
            }
        }
        func assertionName() -> String {
            switch self {
            case .m_requestAccess__config_configcompletion_completion: return ".requestAccess(config:completion:)"
            case .m_mount__apiClient_apiClientconfig_confighealthKitManagerBuilder_healthKitManagerBuildercompletion_completion: return ".mount(apiClient:config:healthKitManagerBuilder:completion:)"
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
        public static func mount(apiClient: Parameter<ActivitySourcesApiClient>, config: Parameter<ActivitySourceConfigBuilder>, healthKitManagerBuilder: Parameter<HealthKitManagerBuildering>, completion: Parameter<(Result<Bool, Error>) -> Void>) -> Verify { return Verify(method: .m_mount__apiClient_apiClientconfig_confighealthKitManagerBuilder_healthKitManagerBuildercompletion_completion(`apiClient`, `config`, `healthKitManagerBuilder`, `completion`))}
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
        public static func mount(apiClient: Parameter<ActivitySourcesApiClient>, config: Parameter<ActivitySourceConfigBuilder>, healthKitManagerBuilder: Parameter<HealthKitManagerBuildering>, completion: Parameter<(Result<Bool, Error>) -> Void>, perform: @escaping (ActivitySourcesApiClient, ActivitySourceConfigBuilder, HealthKitManagerBuildering, @escaping (Result<Bool, Error>) -> Void) -> Void) -> Perform {
            return Perform(method: .m_mount__apiClient_apiClientconfig_confighealthKitManagerBuilder_healthKitManagerBuildercompletion_completion(`apiClient`, `config`, `healthKitManagerBuilder`, `completion`), performs: perform)
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

