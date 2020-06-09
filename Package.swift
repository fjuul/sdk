// swift-tools-version:5.2
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "FjuulSDK",
    products: [
        .library(name: "FjuulSDK", targets: ["FjuulSDK"])
    ],
    dependencies: [
        .package(path: "ios/Core"),
        .package(path: "ios/Analytics")
    ],
    targets: [
        .target(
            name: "FjuulSDK",
            dependencies: ["Core", "Analytics"],
            path: "ios"
        )
    ]
)
