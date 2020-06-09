// swift-tools-version:5.2
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Fjuul",
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
    dependencies: [],
    targets: [
        .target(
            name: "FjuulCore",
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
