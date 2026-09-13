//
//  Azokle_BrowserApp.swift
//  Azokle Browser
//
//  Created by Azokle on 13/09/26.
//

import SwiftUI

@main
struct Azokle_BrowserApp: App {
    init() {
        AzokleFonts.registerCustomFonts()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
