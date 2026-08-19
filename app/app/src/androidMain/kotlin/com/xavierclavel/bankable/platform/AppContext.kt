package com.xavierclavel.bankable.platform

import android.app.Activity
import android.content.Context
import java.lang.ref.WeakReference

/**
 * Android-only holder for the process Context and the foreground Activity.
 *
 * Common code can't take a Context parameter, so the platform actuals below
 * reach for it here instead. [appContext] is set once from
 * BankableApplication.onCreate; [currentActivity] tracks the single Activity so
 * Credential Manager and locale changes have a UI context to work with.
 */
object AppContext {
    lateinit var appContext: Context
        private set

    private var activityRef: WeakReference<Activity>? = null

    val currentActivity: Activity? get() = activityRef?.get()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun setActivity(activity: Activity?) {
        activityRef = activity?.let { WeakReference(it) }
    }
}
