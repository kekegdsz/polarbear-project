package com.undersky.androidim.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object ImMessageNotifier {

    private const val CHANNEL_ID = "im_incoming_messages"

    const val EXTRA_OPEN_PEER_USER_ID = "open_peer_user_id"
    const val EXTRA_OPEN_GROUP_ID = "open_group_id"
    const val EXTRA_OPEN_TITLE_FALLBACK = "open_title_fallback"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_im_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_im_description)
        }
        mgr.createNotificationChannel(channel)
    }

    fun showIncomingMessage(
        context: Context,
        title: String,
        body: String,
        peerUserId: Long,
        groupId: Long
    ) {
        ensureChannel(context)
        val appCtx = context.applicationContext
        val intent = Intent().setClassName(appCtx.packageName, "${appCtx.packageName}.MainActivity").apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_PEER_USER_ID, peerUserId)
            putExtra(EXTRA_OPEN_GROUP_ID, groupId)
            putExtra(EXTRA_OPEN_TITLE_FALLBACK, title)
        }
        val req = ((peerUserId xor groupId) and 0x7fff_ffff).toInt()
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentPi = PendingIntent.getActivity(appCtx, req, intent, piFlags)
        val notification = NotificationCompat.Builder(appCtx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat_24)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(appCtx).notify(req, notification)
    }

    fun cancelAllForApp(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancelAll()
    }
}
