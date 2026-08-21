package com.xavierclavel.bankable.platform

import android.os.Handler
import android.os.Looper
import android.widget.Toast

private val mainHandler = Handler(Looper.getMainLooper())

actual fun showToast(message: String) {
    mainHandler.post {
        Toast.makeText(AppContext.appContext, message, Toast.LENGTH_SHORT).show()
    }
}
