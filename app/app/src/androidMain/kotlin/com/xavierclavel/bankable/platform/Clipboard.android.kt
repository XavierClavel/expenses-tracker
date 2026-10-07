package com.xavierclavel.bankable.platform

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry

actual fun plainTextClipEntry(text: String, sensitive: Boolean): ClipEntry {
    val clipData = ClipData.newPlainText(null, text)
    if (sensitive) {
        // The constant only exists from API 33; older versions honour the same raw key.
        val isSensitiveKey =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ClipDescription.EXTRA_IS_SENSITIVE
            else "android.content.extra.IS_SENSITIVE"
        clipData.description.extras = PersistableBundle().apply { putBoolean(isSensitiveKey, true) }
    }
    return clipData.toClipEntry()
}
