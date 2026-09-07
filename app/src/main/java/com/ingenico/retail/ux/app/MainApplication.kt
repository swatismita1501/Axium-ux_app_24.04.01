/*---------------------------------------------------------------------------------*
 * INGENICO Technical Software Department                                          *
 *---------------------------------------------------------------------------------*
 * Copyright (c) 2025 Ingenico Inc.                                                *
 * 3025 Windward Plaza, Suite 600, Alpharetta, Georgia, 30005, United States       *
 * All rights reserved.                                                            *
 * This source program is the property of the INGENICO Company mentioned above     *
 * and may not be copied in any form or by any means, whether in part or in whole, *
 * except under license expressly granted by such INGENICO company.                *
 * All copies of this source program, whether in part or in whole, and whether     *
 * modified or not, must display this and all other embedded copyright and         *
 * ownership notices in full.                                                      *
 *---------------------------------------------------------------------------------*/

package com.ingenico.retail.ux.app

import android.annotation.SuppressLint
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.ingenico.retail.arc.trace.AndroidLogTraceWriter
import com.ingenico.retail.arc.trace.ArcTrace
import com.ingenico.retail.arc.trace.ArcTraceCategory
import com.ingenico.retail.arc.trace.FileTraceWriter
import com.ingenico.retail.arc.trace.ArcTraceCore
import com.ingenico.retail.types.ui.x.Broadcast
import com.ingenico.retail.ui.x.server.UxIntent
import com.ingenico.retail.ui.x.server.UxScreenManager

class MainApplication : Application() {

    private var trace = ArcTrace(ArcTraceCategory.Ui)

    override fun onCreate() {
        super.onCreate()

        isAxium = detectAxium()
        initializeTrace(applicationContext)

        trace.i { "Application for ux-server is creating" }

        UxScreenManager.initUsdk(applicationContext)

        // Start the ARC Service if configured to start, and was not already started.
        if (Config(baseContext).startArcService)
            startArcService()
    }

    override fun onTerminate() {
        super.onTerminate()
        trace.i { "Application for ux-server is terminating" }
        UxScreenManager.clearUsdk()
    }

    private fun startArcService() {
        // Start the ARC Service in case it was not already started.
        // It will either work on the first attempt or never will (i.e. Service not installed).
        // Never stop the service.
        try {
            trace.i { "Request to start Arc service" }
            applicationContext.startForegroundService(
                Intent().apply {
                    component =
                        ComponentName("com.ingenico.retail.arcapp", "com.ingenico.retail.arcapp.ArcService")
                }
            )
        } catch (e: Exception) {
            // Ignore errors.  ARC may not be the Service version.
        }
    }

    private fun detectAxium(): Boolean = Build.MANUFACTURER == "ingenico"

    private fun initializeTrace(context: Context?) {
        context?.also {
            val apkVersion = context.applicationContext.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            ).versionName

            val config = Config(context.applicationContext)

            with(ArcTraceCore) {
                // File is /storage/self/primary/Android/data/com.ingenico.retail.ux.resources/files/ArcTrace.log
                traceWriter =
                    if (BuildConfig.DEBUG)
                        AndroidLogTraceWriter()
                    else {
                        FileTraceWriter(config.traceFolder) {
                            "**** Start trace file. $apkVersion ********"
                        }
                    }
                enabledCategories = config.traceCategories
                enabledLevel = config.traceLevel
            }
        }
    }

    companion object {
        // Notification constants.
        private const val NOTIFICATION_TAG = "NOTIFICATION_MESSAGE"
        private const val NOTIFICATION_CHANNEL_ID = "channel_ux_111111"
        private const val NOTIFICATION_ID = 111111
        private const val NOTIFICATION_NAME = "Activity opening notification"
        private const val NOTIFICATION_DESCRIPTION = NOTIFICATION_NAME
        private const val NOTIFICATION_TITLE_KEY = "app_name"
        private const val NOTIFICATION_MESSAGE_KEY = "notification_message"
        private const val PENDING_INTENT_CODE = 0
        private const val CUSTOM_NOTIFICATION_LAYOUT_NAME = "notification"

        // Indicator the application is running on AXIUM device or not.
        private var isAxium: Boolean = false

        fun showMainActivity(context: Context, intent: Intent?) {
            if (isAxium || MainActivity.activityOnTop)
                startActivity(context, intent)
            else
                startActivityViaNotification(context, intent)
        }

        // Directly start activity.
        private fun startActivity(context: Context, intent: Intent?) {
            intent?.extras?.getString(Broadcast.JSON_DATA)?.let { jsonMessage ->
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(Broadcast.JSON_DATA, jsonMessage)
                }.also { purpose ->
                    context.startActivity(purpose)
                }
            }
        }

        // Notification method to support opening activity on Standard Android 10 and higher.
        // Suppress warning for usage FLAG_MUTABLE for sdk 29. ux-resources running on AXIUM and Standard Android.
        @SuppressLint("InlinedApi")
        private fun startActivityViaNotification(context: Context, intent: Intent?) {

            val pendingIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(Broadcast.JSON_DATA, UxIntent.getJsonMessage(intent))
            }.let { activityIntent ->
                PendingIntent.getActivity(
                    context,
                    PENDING_INTENT_CODE,
                    activityIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                description = NOTIFICATION_DESCRIPTION
            }

            getLayoutId(context, CUSTOM_NOTIFICATION_LAYOUT_NAME).let { customLayoutId ->
                if (customLayoutId != 0) {

                    // Use custom layout for notification window.
                    val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContent(RemoteViews(context.packageName, customLayoutId))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                    // Show notification window.
                    showNotification(context, channel, builder.build())

                } else {

                    // Use standard Android notification window.
                    val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(getStringByKey(context, NOTIFICATION_TITLE_KEY))
                        .setContentText(getStringByKey(context, NOTIFICATION_MESSAGE_KEY))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .let {
                            NotificationCompat.BigTextStyle(it)
                                .bigText(getStringByKey(context, NOTIFICATION_TITLE_KEY))
                                .setBigContentTitle(getStringByKey(context, NOTIFICATION_MESSAGE_KEY))
                        }
                    // Show notification window.
                    showNotification(context, channel, builder.build())

                }
            }
        }

        private fun showNotification(context: Context, channel: NotificationChannel, notification: Notification?) =
            with(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
                createNotificationChannel(channel)
                notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification)
            }

        private fun getStringByKey(context: Context, stringKey: String): String =
            "${context.packageName}:string/$stringKey".let { resourceName ->
                context.resources.getIdentifier(resourceName, null, null).let { id ->
                    if (id == 0) "Internal Error!"
                    else context.resources.getString(id)
                }
            }

        private fun getLayoutId(context: Context, resourceEntryName: String): Int =
            "${context.packageName}:layout/$resourceEntryName".let {
                context.resources.getIdentifier(it, null, null)
            }
    }
}