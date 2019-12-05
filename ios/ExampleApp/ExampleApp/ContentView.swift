//
//  ContentView.swift
//  ExampleApp
//
//  Created by Leo on 05.12.19.
//  Copyright © 2019 Fjuul Vision Oy. All rights reserved.
//

import SwiftUI
import Analytics

struct ContentView: View {
    var body: some View {
        let analytics = Analytics()
        return Text(analytics.text())
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
