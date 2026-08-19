import SwiftUI
import UIKit
import ComposeApp

/// Hosts the shared Compose UI. `MainViewController()` comes from the Kotlin
/// `ComposeApp` framework (app/src/iosMain/.../MainViewController.kt).
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            // Compose draws its own insets, including behind the keyboard.
            .ignoresSafeArea(.all)
    }
}
