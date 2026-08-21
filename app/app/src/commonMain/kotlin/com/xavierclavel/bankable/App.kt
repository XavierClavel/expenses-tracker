package com.xavierclavel.bankable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.xavierclavel.bankable.navigation.AppNavigation
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.platform.rememberAppLocaleTag
import com.xavierclavel.bankable.ui.theme.MyApplicationTheme

/**
 * The whole app UI, shared by every platform. Each platform entry point
 * (MainActivity on Android, MainViewController on iOS) does its own
 * window-level setup and then hosts this.
 */
@Composable
fun BankableApp() {
    CompositionLocalProvider(LocalAppLocale provides rememberAppLocaleTag()) {
        MyApplicationTheme {
            AppNavigation()
        }
    }
}
