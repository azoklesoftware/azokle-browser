// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "AzokleBrowserMac",
    platforms: [
        .macOS(.v13)
    ],
    products: [
        .executable(name: "AzokleBrowserMac", targets: ["AzokleBrowserMac"])
    ],
    dependencies: [],
    targets: [
        .target(
            name: "AzokleUniFFI",
            path: "../../bindings/swift",
            publicHeadersPath: "."
        ),
        .executableTarget(
            name: "AzokleBrowserMac",
            dependencies: ["AzokleUniFFI"],
            path: "src"
        )
    ]
)
