package com.xavierclavel.bankable.platform

import com.xavierclavel.bankable.data.Sync
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow
import kotlin.coroutines.cancellation.CancellationException

// Also listed under BGTaskSchedulerPermittedIdentifiers in iosApp/Info.plist.
private const val SYNC_TASK = "com.xavierclavel.bankable.sync"

// iOS may not run a refresh before this delay: long enough to batch the changes of a session.
private const val EARLIEST_DELAY_SECONDS = 15.0 * 60

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Asks for a background app refresh; iOS decides when, from how the app is used. */
@OptIn(ExperimentalForeignApi::class)
actual fun scheduleBackgroundSync() {
    val request = BGAppRefreshTaskRequest(identifier = SYNC_TASK)
    request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(EARLIEST_DELAY_SECONDS)
    BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error = null)
}

actual fun cancelBackgroundSync() {
    BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(SYNC_TASK)
}

/** Tells iOS how to run the background sync. Must happen before the app finishes launching. */
internal fun registerBackgroundSync() {
    BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(SYNC_TASK, usingQueue = null) { task ->
        if (task != null) runSyncTask(task)
    }
}

private fun runSyncTask(task: BGTask) {
    val job = scope.launch {
        try {
            val done = Sync.runInBackground()
            // A refresh that failed (offline) asks for another one.
            if (!done) scheduleBackgroundSync()
            task.setTaskCompletedWithSuccess(done)
        } catch (e: CancellationException) {
            // Expired: the expiration handler has completed the task.
            throw e
        }
    }
    task.expirationHandler = {
        job.cancel()
        scheduleBackgroundSync()
        task.setTaskCompletedWithSuccess(false)
    }
}
