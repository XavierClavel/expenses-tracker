package com.xavierclavel.bankable.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSThread
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

private const val NANOS_PER_SECOND = 1_000_000_000.0
private const val VISIBLE_SECONDS = 2.0
private const val FADE_SECONDS = 0.25

/**
 * iOS has no system toast, so this fades a rounded label in over the key window
 * and fades it back out — the same "brief, non-blocking confirmation" role
 * Toast plays on Android.
 */
actual fun showToast(message: String) {
    if (NSThread.isMainThread()) {
        presentToast(message)
    } else {
        dispatch_async(dispatch_get_main_queue()) { presentToast(message) }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun presentToast(message: String) {
    val window = keyWindow() ?: return
    val label = UILabel().apply {
        text = message
        textAlignment = NSTextAlignmentCenter
        textColor = UIColor.whiteColor
        backgroundColor = UIColor.blackColor.colorWithAlphaComponent(0.8)
        numberOfLines = 0
        alpha = 0.0
        clipsToBounds = true
        layer.cornerRadius = 12.0
    }

    val (windowWidth, windowHeight) = window.bounds.useContents { size.width to size.height }
    val labelWidth = windowWidth - 64.0
    val textHeight = label.sizeThatFits(CGSizeMake(labelWidth, 200.0)).useContents { height }
    val labelHeight = maxOf(textHeight + 24.0, 44.0)
    label.setFrame(
        CGRectMake(
            x = 32.0,
            y = windowHeight - labelHeight - 96.0,
            width = labelWidth,
            height = labelHeight,
        )
    )

    window.addSubview(label)
    UIView.animateWithDuration(FADE_SECONDS, animations = { label.alpha = 1.0 })
    dispatch_after(
        dispatch_time(DISPATCH_TIME_NOW, (VISIBLE_SECONDS * NANOS_PER_SECOND).toLong()),
        dispatch_get_main_queue(),
    ) {
        UIView.animateWithDuration(
            duration = FADE_SECONDS,
            animations = { label.alpha = 0.0 },
            completion = { label.removeFromSuperview() },
        )
    }
}

private fun keyWindow(): UIWindow? {
    val scenes = UIApplication.sharedApplication.connectedScenes
    val windows = scenes.filterIsInstance<UIWindowScene>().flatMap { it.windows.filterIsInstance<UIWindow>() }
    return windows.firstOrNull { it.isKeyWindow() } ?: windows.firstOrNull()
}
