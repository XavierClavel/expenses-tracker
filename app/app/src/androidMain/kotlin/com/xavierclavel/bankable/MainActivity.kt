package com.xavierclavel.bankable

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
        // The UI is dark regardless of the system theme, so keep bar icons light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            BankableApp()
        }
    }

    override fun onDestroy() {
        if (AppContext.currentActivity === this) AppContext.setActivity(null)
        super.onDestroy()
    }
}
