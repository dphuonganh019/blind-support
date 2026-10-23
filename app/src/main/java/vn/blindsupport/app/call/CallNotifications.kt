package vn.blindsupport.app.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import vn.blindsupport.app.R

object CallNotifications {
    const val CH_INCOMING = "calls_incoming"
    const val CH_ONGOING = "calls_ongoing"
    const val CH_VOIP = "voip_incoming"
    const val CH_STATUS = "status"
    private const val ID_CALL = 1001
    const val ID_VOIP = 1002

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_INCOMING, "Cuộc gọi đến", NotificationManager.IMPORTANCE_HIGH).apply {
                // Chuông do hệ thống phát, kênh này không phát thêm âm thanh.
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ONGOING, "Cuộc gọi đang diễn ra", NotificationManager.IMPORTANCE_LOW).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_VOIP, "Cuộc gọi Zalo/WhatsApp", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_STATUS, "Trạng thái", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun inCallPending(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun action(context: Context, action: String, req: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, req,
            Intent(context, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun showIncoming(context: Context, name: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val full = inCallPending(context)
        val n = Notification.Builder(context, CH_INCOMING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Cuộc gọi đến")
            .setContentText(name)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(full)
            .setFullScreenIntent(full, true)
            .addAction(Notification.Action.Builder(null, "Nghe", action(context, CallActionReceiver.ANSWER, 1)).build())
            .addAction(Notification.Action.Builder(null, "Từ chối", action(context, CallActionReceiver.DECLINE, 2)).build())
            .build()
        runCatching { nm.notify(ID_CALL, n) }
    }

    fun showOngoing(context: Context, name: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val n = Notification.Builder(context, CH_ONGOING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Đang gọi")
            .setContentText(name)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(inCallPending(context))
            .addAction(Notification.Action.Builder(null, "Kết thúc", action(context, CallActionReceiver.HANGUP, 3)).build())
            .build()
        runCatching { nm.notify(ID_CALL, n) }
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(ID_CALL)
    }
}
