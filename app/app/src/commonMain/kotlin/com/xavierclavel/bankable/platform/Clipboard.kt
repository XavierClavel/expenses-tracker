package com.xavierclavel.bankable.platform

import androidx.compose.ui.platform.ClipEntry

/**
 * A plain-text entry for Compose's `LocalClipboard`. The clipboard itself is multiplatform,
 * but a [ClipEntry] can only be built from platform types (ClipData on Android, pasteboard
 * text on iOS).
 *
 * Set [sensitive] for secrets: Android 13+ then masks the text in its "copied" preview.
 * iOS has no equivalent flag, so it is ignored there.
 */
expect fun plainTextClipEntry(text: String, sensitive: Boolean = false): ClipEntry
