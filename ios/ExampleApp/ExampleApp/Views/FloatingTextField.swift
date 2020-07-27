import SwiftUI

struct FloatingTextField: View {

    let title: String
    let text: Binding<String>

    var body: some View {
        ZStack(alignment: .leading) {
            Text(title)
                .foregroundColor(Color(.placeholderText))
                .offset(y: text.wrappedValue.isEmpty ? 0 : -25)
                .scaleEffect(text.wrappedValue.isEmpty ? 1 : 0.8, anchor: .leading)
            TextField("", text: text)
        }
        .padding(.top, 15)
        .animation(.spring(response: 0.2, dampingFraction: 0.5))
    }

}
