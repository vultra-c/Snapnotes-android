package com.whyy.snapnotes.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationPermissionCompat {
    const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

    fun shouldRequestPostNotificationsPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    fun hasPostNotificationsPermission(context: Context): Boolean {
        return !shouldRequestPostNotificationsPermission() ||
            ContextCompat.checkSelfPermission(
                context,
                POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun canPostNotifications(context: Context): Boolean {
        return hasPostNotificationsPermission(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
