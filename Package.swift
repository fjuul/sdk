// swift-tools-version:5.2
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Fjuul",
    products: [
        .library(
            name: "FjuulCore",
            targets: ["Core"]
        ),
        .library(
            name: "FjuulAnalytics",
            targets: ["Analytics"]
        )
    ],
    dependencies: [],
    targets: [
        .target(
            name: "Core",
            path: "ios/Core/Sources"
        ),
        .testTarget(
            name: "CoreTests",
            dependencies: ["Core"],
            path: "ios/Core/Tests"
        ),
        .target(
            name: "Analytics",
            dependencies: ["Core"],
            path: "ios/Analytics/Sources"
        ),
        .testTarget(
            name: "AnalyticsTests",
            dependencies: ["Analytics"],
            path: "ios/Analytics/Tests"
        )
    ]
)
