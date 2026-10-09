import SwiftUI
import UIKit

/// Shows the signed-in user's credentials for development and testing, since signing out clears them from onboarding.
struct CredentialsScreen: View {

    let credentials: Credentials

    var body: some View {
        Form {
            Section(footer: Text("Long-press a value to copy it.")) {
                CredentialRow(label: "Base URL", value: credentials.baseUrl)
                CredentialRow(label: "API key", value: credentials.apiKey)
                CredentialRow(label: "Token", value: credentials.token)
                CredentialRow(label: "Secret", value: credentials.secret)
            }
        }
        .navigationBarTitle("Credentials", displayMode: .inline)
    }

}

private struct CredentialRow: View {

    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
            Text(value)
                .font(.system(.body, design: .monospaced))
        }
        .contextMenu {
            Button(action: { UIPasteboard.general.string = value }) {
                Text("Copy")
            }
        }
    }

}
