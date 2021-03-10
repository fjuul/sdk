import SwiftUI

struct CheckboxField: View {
    let id: Int
    let label: String
    let size: CGFloat
    let color: Color
    let textSize: Int
    let callback: (Int, Bool) -> Void

    init(id: Int, label: String, size: CGFloat = 10, color: Color = Color.black, textSize: Int = 16, callback: @escaping (Int, Bool) -> Void) {
        self.id = id
        self.label = label
        self.size = size
        self.color = color
        self.textSize = textSize
        self.callback = callback
    }

    @State var isMarked: Bool = true

    var body: some View {
        Button(action: {
            self.isMarked.toggle()
            self.callback(self.id, self.isMarked)
        }) {
            HStack(alignment: .center, spacing: 10) {
                Image(systemName: self.isMarked ? "checkmark.square" : "square")
                    .renderingMode(.original)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: self.size, height: self.size)
                Text(label)
                    .font(Font.system(size: size))
                Spacer()
            }.foregroundColor(self.color)
        }
        .buttonStyle(BorderlessButtonStyle())
        .foregroundColor(Color.white)
    }
}
