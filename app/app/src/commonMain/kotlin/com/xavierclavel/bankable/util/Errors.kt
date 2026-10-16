package com.xavierclavel.bankable.util

import com.xavierclavel.bankable.data.CategoryInUseException
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.error_category_in_use
import org.jetbrains.compose.resources.getString

/** The message to show for [e]: a translated one for the failures the app expects, the raw one otherwise. */
suspend fun userMessage(e: Exception, fallback: String): String = when (e) {
    is CategoryInUseException -> getString(Res.string.error_category_in_use)
    else -> e.message ?: fallback
}
