import SwiftUI
import FjuulAnalytics

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
