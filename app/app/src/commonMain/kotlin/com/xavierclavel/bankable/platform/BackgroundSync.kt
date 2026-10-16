package com.xavierclavel.bankable.platform

/**
 * Asks the system to run a sync in the background, so that changes made on the device reach the
 * server even if the app is closed first. Android runs it with WorkManager as soon as a network
 * is available; iOS runs a background app refresh task when it sees fit.
 */
expect fun scheduleBackgroundSync()

/** Drops the background sync scheduled, if any. Run on logout. */
expect fun cancelBackgroundSync()
