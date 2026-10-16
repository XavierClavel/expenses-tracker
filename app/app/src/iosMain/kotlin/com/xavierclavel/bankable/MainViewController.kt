package com.xavierclavel.bankable

import androidx.compose.ui.window.ComposeUIViewController
import com.xavierclavel.bankable.api.initHttpClient
import com.xavierclavel.bankable.data.initDatabase
import com.xavierclavel.bankable.platform.registerBackgroundSync
import platform.UIKit.UIViewController

private var initialized = false

/**
 * Sets up the HTTP client and the database, once per process: the app may be launched in the
 * background, for a sync, without ever showing its UI.
 */
private fun initApp() {
    if (initialized) return
    initialized = true
    initHttpClient()
    initDatabase()
}

/**
 * Called from iosApp/iOSApp.swift when the app launches, before it finishes launching as iOS
 * requires for registering background tasks.
 */
fun onAppLaunch() {
    initApp()
    registerBackgroundSync()
}

/**
 * Entry point for the iOS app: iosApp/ContentView.swift wraps this
 * UIViewController in a SwiftUI view.
 */
fun MainViewController(): UIViewController {
    initApp()
    return ComposeUIViewController {
        BankableApp()
    }
}
