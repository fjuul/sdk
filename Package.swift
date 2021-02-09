// swift-tools-version:5.2
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Fjuul SDK",
    platforms: [
        // required to run tests depending on Alamofire
        .macOS(.v10_12),
        .iOS(.v10)
    ],
    products: [
        .library(name: "FjuulCore", targets: ["FjuulCore"]),
        .library(name: "FjuulActivitySources", targets: ["FjuulActivitySources"]),
        .library(name: "FjuulAnalytics", targets: ["FjuulAnalytics"]),
        .library(name: "FjuulUser", targets: ["FjuulUser"])
    ],
    dependencies: [
        .package(url: "https://github.com/Alamofire/Alamofire.git", .upToNextMajor(from: "5.2.0")),
        .package(url: "https://github.com/AliSoftware/OHHTTPStubs.git", .upToNextMajor(from: "9.0.0")),
        .package(name: "swiftymocky", url: "https://github.com/MakeAWishFoundation/SwiftyMocky", from: "4.0.1"),
        .package(url: "https://github.com/apple/swift-log.git", from: "1.0.0"),
    ],
    targets: [
        .target(
            name: "FjuulCore",
            dependencies: [
                "Alamofire",
                .product(name: "Logging", package: "swift-log")
            ],
            path: "ios/Core/Sources"
        ),
        .testTarget(
            name: "FjuulCoreTests",
            dependencies: [.product(name: "OHHTTPStubsSwift", package: "OHHTTPStubs"), "FjuulCore"],
            path: "ios/Core/Tests"
        ),
        .target(
            name: "FjuulActivitySources",
            dependencies: [
                "FjuulCore",
                .product(name: "Logging", package: "swift-log")
            ],
            path: "ios/ActivitySources/Sources"
        ),
        .testTarget(
            name: "FjuulActivitySourcesTests",
            dependencies: [
                .product(name: "OHHTTPStubsSwift", package: "OHHTTPStubs"),
                .product(name: "SwiftyMocky", package: "swiftymocky"),
                "FjuulActivitySources"
            ],
            path: "ios/ActivitySources/Tests"
        ),
        .target(
            name: "FjuulAnalytics",
            dependencies: [
                "FjuulCore",
                .product(name: "Logging", package: "swift-log")
            ],
            path: "ios/Analytics/Sources"
        ),
        .testTarget(
            name: "FjuulAnalyticsTests",
            dependencies: [.product(name: "OHHTTPStubsSwift", package: "OHHTTPStubs"), "FjuulAnalytics"],
            path: "ios/Analytics/Tests"
        ),
        .target(
            name: "FjuulUser",
            dependencies: ["FjuulCore"],
            path: "ios/User/Sources"
        ),
        .testTarget(
            name: "FjuulUserTests",
            dependencies: [.product(name: "OHHTTPStubsSwift", package: "OHHTTPStubs"), "FjuulUser"],
            path: "ios/User/Tests"
        )
    ],
    swiftLanguageVersions: [.v5]
)
