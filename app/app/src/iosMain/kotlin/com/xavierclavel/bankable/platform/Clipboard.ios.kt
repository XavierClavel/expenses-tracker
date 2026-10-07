package com.xavierclavel.bankable.platform

import androidx.compose.ui.platform.ClipEntry

actual fun plainTextClipEntry(text: String, sensitive: Boolean): ClipEntry =
    ClipEntry.withPlainText(text)
