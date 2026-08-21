package com.xavierclavel.bankable

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.xavierclavel.bankable.platform.AppContext
import com.xavierclavel.bankable.settings.AndroidLocaleManager

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AndroidLocaleManager.applyStoredLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Credential Manager and the language picker need a live Activity.
        AppContext.setActivity(this)
        enableEdgeToEdge()
        setContent {
            BankableApp()
        }
    }

    override fun onDestroy() {
        if (AppContext.currentActivity === this) AppContext.setActivity(null)
        super.onDestroy()
    }
}
