package com.xavierclavel.bankable.platform

/**
 * Shows a short, transient message: a real Toast on Android, an equivalent
 * self-dismissing overlay on iOS (which has no system toast).
 */
expect fun showToast(message: String)
