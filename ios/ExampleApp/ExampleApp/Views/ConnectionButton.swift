//
//  ConnectionButton.swift
//  ExampleApp
//

import SwiftUI

struct ConnectionButton: View {
    var text: String
    var body: some View {
        Button(action: {
            print(123)
//            self.activitySourceObserver.connect(activitySource: ActivitySourceFitbit.shared)
        }) {
            Text(text)
        }
    }
}

struct ConnectionButton_Previews: PreviewProvider {
    static var previews: some View {
        ConnectionButton(text: "Connect Healthkit")
    }
}
