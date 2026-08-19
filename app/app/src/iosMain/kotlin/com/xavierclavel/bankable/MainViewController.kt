package com.xavierclavel.bankable

import androidx.compose.ui.window.ComposeUIViewController
import com.xavierclavel.bankable.api.initHttpClient
import platform.UIKit.UIViewController

/**
 * Entry point for the iOS app: iosApp/ContentView.swift wraps this
 * UIViewController in a SwiftUI view.
 */
fun MainViewController(): UIViewController {
    initHttpClient()
    return ComposeUIViewController {
        BankableApp()
    }
}
