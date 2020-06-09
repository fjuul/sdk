// swift-tools-version:5.2
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Analytics",
    products: [
        .library(
            name: "Analytics",
            targets: ["Analytics"]
        ),
    ],
    dependencies: [
        .package(path: "../Core"),
    ],
    targets: [
        .target(
            name: "Analytics",
            dependencies: ["Core"],
            path: "Sources"
        ),
        .testTarget(
            name: "AnalyticsTests",
            dependencies: ["Analytics"],
            path: "Tests"
        ),
    ]
)
