// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "AzokleBrowserIos",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .library(name: "AzokleBrowserIos", targets: ["AzokleBrowserIos"])
    ],
    dependencies: [],
    targets: [
        .target(
            name: "AzokleUniFFI",
            path: "../../bindings/swift",
            publicHeadersPath: "."
        ),
        .target(
            name: "AzokleBrowserIos",
            dependencies: ["AzokleUniFFI"],
            path: "src"
        )
    ]
)
