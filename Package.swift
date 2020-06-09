// swift-tools-version:5.2
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "FjuulSDK",
    products: [
        .library(
            name: "Core",
            targets: ["Core"]
        ),
        .library(
            name: "Analytics",
            targets: ["Analytics"]
        )
    ],
    dependencies: [],
    targets: [
        .target(
            name: "Core",
            path: "ios/Core/Sources"
        ),
        .target(
            name: "Analytics",
            dependencies: ["Core"],
            path: "ios/Analytics/Sources"
        ),
        .testTarget(
            name: "CoreTests",
            dependencies: ["Core"],
            path: "ios/Core/Tests"
        ),
        .testTarget(
            name: "AnalyticsTests",
            dependencies: ["Analytics"],
            path: "ios/Analytics/Tests"
        )
    ]
)
