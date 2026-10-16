package com.xavierclavel.bankable.platform

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.xavierclavel.bankable.data.Sync
import java.util.concurrent.TimeUnit

private const val SYNC_WORK = "sync"

/** Runs a sync once a network is available, retrying with a growing delay while it fails. */
actual fun scheduleBackgroundSync() {
    val request = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()
    // Replacing restarts a run in flight, so that it also sends the changes made since it started.
    WorkManager.getInstance(AppContext.appContext).enqueueUniqueWork(SYNC_WORK, ExistingWorkPolicy.REPLACE, request)
}

actual fun cancelBackgroundSync() {
    WorkManager.getInstance(AppContext.appContext).cancelUniqueWork(SYNC_WORK)
}

/**
 * The background sync. WorkManager may start the process just for it: BankableApplication has
 * then set up the HTTP client and the database, and [Sync.runInBackground] loads the session.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        if (Sync.runInBackground()) Result.success() else Result.retry()
}
