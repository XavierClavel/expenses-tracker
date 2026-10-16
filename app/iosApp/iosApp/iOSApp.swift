import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // Registers the background sync, which iOS requires before launch completes.
        MainViewControllerKt.onAppLaunch()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
