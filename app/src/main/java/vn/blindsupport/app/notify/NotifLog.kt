package vn.blindsupport.app.notify

import android.app.Notification
import android.service.notification.StatusBarNotification
import vn.blindsupport.app.core.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Nhật ký ngắn về thông báo Zalo/WhatsApp app nhận được, lúc nhận, và việc app đã làm.
 * Người hỗ trợ gửi nhật ký này cho người làm app khi cuộc gọi/tin nhắn Zalo không được báo,
 * để biết Zalo đưa thông báo lên thế nào và Zalo trễ hay app trễ.
 */
object NotifLog {
    private const val MAX_LINES = 150

    @Synchronized
    fun add(line: String) {
        runCatching {
            val time = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())
            val lines = (Prefs.notifLog.lines().filter { it.isNotBlank() } + "$time $line").takeLast(MAX_LINES)
            Prefs.notifLog = lines.joinToString("\n")
        }
    }

    /** Mô tả gọn một thông báo: loại, cờ, các nút, tiêu đề, nội dung, độ trễ. */
    fun describe(sbn: StatusBarNotification): String = runCatching {
        val n = sbn.notification
        val now = System.currentTimeMillis()
        val e = n.extras
        val template = e.getString(Notification.EXTRA_TEMPLATE)?.substringAfterLast('$')?.substringAfterLast('.')
        val flags = buildList {
            if ((n.flags and Notification.FLAG_ONGOING_EVENT) != 0) add("ongoing")
            if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) add("summary")
            if (n.fullScreenIntent != null) add("fullscreen")
            if (n.contentIntent != null) add("content")
        }
        val actions = n.actions?.joinToString("|") { it.title?.toString().orEmpty() }.orEmpty()
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().take(40)
        val text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().take(40)
        val sub = e.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty().take(30)
        val postDelay = (now - sbn.postTime) / 1000
        val whenDelay = if (n.`when` > 0) (now - n.`when`) / 1000 else -1
        buildString {
            append(AppIds.label(sbn.packageName))
            append(" #").append(sbn.id)
            sbn.tag?.let { append(" tag=").append(it.take(20)) }
            append(" cat=").append(n.category ?: "-")
            append(" kiểu=").append(template ?: "-")
            if (flags.isNotEmpty()) append(" [").append(flags.joinToString(",")).append(']')
            if (actions.isNotEmpty()) append(" nút=").append(actions)
            append(" | ").append(title)
            append(" | ").append(text)
            if (sub.isNotEmpty()) append(" | sub=").append(sub)
            append(" | đăng trước ").append(postDelay).append("s, giờ tin trước ").append(whenDelay).append('s')
        }
    }.getOrElse { "lỗi đọc thông báo: ${it.message}" }
}
