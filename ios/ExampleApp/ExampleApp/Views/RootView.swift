import SwiftUI

enum PresentedView {
   case onboarding
   case moduleSelection
}

class ViewRouter: ObservableObject {

    @Published var presentedView: PresentedView = .onboarding

}

struct RootView: View {

    @EnvironmentObject var userDefaultsManager: UserDefaultsManager
    @ObservedObject var viewRouter = ViewRouter()

    var body: some View {
        NavigationView {
            VStack {
                if viewRouter.presentedView == .onboarding {
                    OnboardingView(viewRouter: viewRouter)
                } else if viewRouter.presentedView == .moduleSelection {
                    ModuleSelectionView()
                }
            }
        }
    }

}

struct RootView_Previews: PreviewProvider {
    static var previews: some View {
        RootView()
    }
}
