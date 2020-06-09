import Core

public struct Analytics {
    
    let core = Core()
    
    public init() {}
    
    public func text() -> String {
        return core.text
    }
    
}
