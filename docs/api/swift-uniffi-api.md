# Swift UniFFI API Reference

Generated Bindings: `bindings/swift/azokle_uniffi.swift`
Rust Crate: `bindings/uniffi`

---

## 1. Types & Records

### `UniversalFilterAction`
```swift
public enum UniversalFilterAction: Equatable, Hashable {
    case allow
    case block
    case redirect
}
```

### `UniversalFilterDecision`
```swift
public struct UniversalFilterDecision {
    public var action: UniversalFilterAction
    public var category: String?
    public var redirectUrl: String?
    public var reason: String
}
```

### `UniversalShieldStats`
```swift
public struct UniversalShieldStats {
    public var totalBlocked: UInt64
    public var adsBlocked: UInt64
    public var trackersBlocked: UInt64
    public var telemetryBlocked: UInt64
    public var annoyancesBlocked: UInt64
    public var fingerprintingBlocked: UInt64
    public var cryptominersBlocked: UInt64
}
```

---

## 2. `UniversalBrowserEngine` Class

Main Swift class wrapping the Rust core engine instance.

```swift
public class UniversalBrowserEngine {
    public init()
    
    /// Evaluates whether a network URL should be blocked or allowed
    public func shouldBlock(
        url: String, 
        sourceOrigin: String?, 
        requestType: String?, 
        isThirdParty: Bool
    ) -> UniversalFilterDecision
    
    /// Strips tracking parameters from an arbitrary URL string
    public func stripTrackingParams(url: String) -> String
    
    /// Unwraps bounce tracking redirect hops
    public func checkDebounce(url: String) -> String?
    
    /// Fetches live shield metrics
    public func getStats() -> UniversalShieldStats
    
    /// Enables or disables shield protection for a specific host
    public func setHostShieldEnabled(host: String, enabled: Bool)
}
```

### Swift Usage Example
```swift
import SwiftUI
import AzokleUniFFI

class BrowserController: ObservableObject {
    private let engine = UniversalBrowserEngine()
    @Published var blockedCount: UInt64 = 0

    func inspectRequest(url: String, origin: String?) -> Bool {
        let decision = engine.shouldBlock(
            url: url,
            sourceOrigin: origin,
            requestType: "script",
            isThirdParty: true
        )
        
        if decision.action == .block {
            self.blockedCount = engine.getStats().totalBlocked
            return false // Deny request
        }
        return true // Allow request
    }
}
```
