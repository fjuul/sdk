import SwiftUI

struct UserProfileScreen: View {

    @Environment(\.presentationMode) var presentation
    @ObservedObject var userProfile = UserProfileObservable(fetchOnInit: true)

    @State private var showingLogoutAlert = false
    @State private var onSuccessLogout = false
    @State private var showingUserDeleteAlert = false

    var body: some View {
        VStack {
            NavigationLink(destination: RootView(), isActive: self.$onSuccessLogout) {
                Text("")
            }
            .frame(width: 0, height: 0)
            .padding(0)

            Form {
                Section {
                    UserProfileForm(showOptionalFields: true).environmentObject(userProfile)
                }
                Section {
                    Button(action: {
                        self.showingUserDeleteAlert = true
                    }) {
                        Text("Delete profile")
                    }.foregroundColor(.red)
                    .background(Color.white)
                    .alert(isPresented: $showingUserDeleteAlert) {
                        Alert(title: Text("Are you sure you want to mark your profile for deletion?"), primaryButton: .destructive(Text("Delete")) {
                            if userProfile.markUserForDeletion() {
                                self.onSuccessLogout = true
                            }
                        }, secondaryButton: .cancel())
                    }
                }
                Section {
                    Button("Update profile") {
                        self.userProfile.updateUser { result in
                            switch result {
                            case .success: self.presentation.wrappedValue.dismiss()
                            case .failure: break
                            }
                        }
                    }
                }
            }
            .navigationBarTitle("User Profile", displayMode: .inline)

            Section {
                Button(action: {
                    self.showingLogoutAlert = true
                }) {
                    Text("Logout")
                        .frame(maxWidth: .infinity)
                        .padding(10)
                }.foregroundColor(.white)
                .background(Color.red)
                .cornerRadius(10)
                .padding(10)
                .alert(isPresented: $showingLogoutAlert) {
                    Alert(title: Text("Are you sure you want to logout?"), primaryButton: .destructive(Text("Logout")) {
                        if userProfile.logout() {
                           self.onSuccessLogout = true
                        }
                    }, secondaryButton: .cancel())
                }
            }
        }

    }
}

struct UserProfileScreen_Previews: PreviewProvider {
    static var previews: some View {
        UserProfileScreen()
    }
}
