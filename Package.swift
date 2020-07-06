// swift-tools-version:5.2
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Fjuul",
    platforms: [
        // required to run tests depending on Alamofire
        .macOS(.v10_12),
        .iOS(.v10)
    ],
    products: [
        .library(
            name: "FjuulCore",
            targets: ["FjuulCore"]
        ),
        .library(
            name: "FjuulAnalytics",
            targets: ["FjuulAnalytics"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/Alamofire/Alamofire.git", .upToNextMajor(from: "5.2.0"))
    ],
    targets: [
        .target(
            name: "FjuulCore",
            dependencies: ["Alamofire"],
            path: "ios/Core/Sources"
        ),
        .testTarget(
            name: "FjuulCoreTests",
            dependencies: ["FjuulCore"],
            path: "ios/Core/Tests"
        ),
        .target(
            name: "FjuulAnalytics",
            dependencies: ["FjuulCore"],
            path: "ios/Analytics/Sources"
        ),
        .testTarget(
            name: "FjuulAnalyticsTests",
            dependencies: ["FjuulAnalytics"],
            path: "ios/Analytics/Tests"
        )
    ]
)
