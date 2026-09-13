//
//  ContentView.swift
//  Azokle Browser
//
//  Created by Azokle on 13/09/26.
//

import SwiftUI

public struct ContentView: View {
    public var body: some View {
        BrowserScreen()
            .preferredColorScheme(.dark)
    }
}

#Preview {
    ContentView()
}
