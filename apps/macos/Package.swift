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
            name: "azokle_uniffiFFI",
            path: "../../bindings/swift",
            exclude: ["azokle_uniffi.swift"],
            publicHeadersPath: "."
        ),
        .target(
            name: "AzokleUniFFI",
            dependencies: ["azokle_uniffiFFI"],
            path: "../../bindings/swift",
            exclude: ["azokle_uniffiFFI.h", "azokle_uniffiFFI.modulemap"],
            sources: ["azokle_uniffi.swift"]
        ),
        .executableTarget(
            name: "AzokleBrowserMac",
            dependencies: ["AzokleUniFFI"],
            path: "src"
        )
    ]
)
