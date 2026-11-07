package vn.blindsupport.app.notify

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import vn.blindsupport.app.core.Prefs
import vn.blindsupport.app.core.StoredMessage
import vn.blindsupport.app.core.TextUtils

object AppIds {
    const val ZALO = "com.zing.zalo"
    val WHATSAPP = setOf("com.whatsapp", "com.whatsapp.w4b")
    val SMS = setOf(
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.android.messaging",
    )
    val VOIP = WHATSAPP + ZALO

    fun label(pkg: String): String = when {
        pkg == ZALO -> "Zalo"
        pkg in WHATSAPP -> "WhatsApp"
        pkg in SMS -> "tin nhắn"
        else -> "tin nhắn"
    }

    fun readEnabled(pkg: String): Boolean = when {
        pkg == ZALO -> Prefs.readZalo
        pkg in WHATSAPP -> Prefs.readWhatsapp
        pkg in SMS -> Prefs.readSms
        else -> false
    }
}

/** Thông tin một cuộc gọi Zalo/WhatsApp lấy từ thông báo. */
data class VoipCallInfo(
    val key: String,
    val pkg: String,
    val callerName: String,
    val answer: PendingIntent?,
    val decline: PendingIntent?,
)

@Suppress("DEPRECATION")
private inline fun <reified T : Parcelable> Bundle.parcel(key: String): T? =
    if (Build.VERSION.SDK_INT >= 33) getParcelable(key, T::class.java) else getParcelable<Parcelable>(key) as? T

object NotificationParser {

    private val ANSWER_KEYS = listOf("trả lời", "nghe", "answer", "accept", "chấp nhận", "bắt máy", "nhận")
    private val DECLINE_KEYS = listOf("từ chối", "decline", "reject", "bỏ qua", "dismiss", "kết thúc", "hang up", "gác máy")
    private const val CATEGORY_MISSED_CALL = "missed_call"

    private fun matches(title: CharSequence?, keys: List<String>): Boolean {
        val t = " " + TextUtils.normalize(title?.toString() ?: "") + " "
        return keys.any { t.contains(" " + TextUtils.normalize(it) + " ") }
    }

    private fun isCallNotification(n: Notification): Boolean =
        n.category == Notification.CATEGORY_CALL ||
            (Build.VERSION.SDK_INT >= 31 && n.extras.containsKey(Notification.EXTRA_CALL_TYPE))

    /** Cuộc gọi Zalo/WhatsApp ĐANG ĐỔ CHUÔNG (có nút trả lời), hoặc null. */
    fun incomingVoipCall(sbn: StatusBarNotification): VoipCallInfo? {
        if (sbn.packageName !in AppIds.VOIP) return null
        val n = sbn.notification ?: return null
        val looksLikeCall = isCallNotification(n) || n.fullScreenIntent != null
        if (!looksLikeCall) return null

        var answer: PendingIntent? = n.actions?.firstOrNull { matches(it.title, ANSWER_KEYS) }?.actionIntent
        var decline: PendingIntent? = n.actions?.firstOrNull { matches(it.title, DECLINE_KEYS) }?.actionIntent
        var incomingByStyle = false
        if (Build.VERSION.SDK_INT >= 31) {
            val type = n.extras.getInt(Notification.EXTRA_CALL_TYPE, 0)
            if (type == Notification.CallStyle.CALL_TYPE_INCOMING) incomingByStyle = true
            if (answer == null) {
                answer = n.extras.parcel<PendingIntent>(Notification.EXTRA_ANSWER_INTENT)
            }
            if (decline == null) {
                decline = n.extras.parcel<PendingIntent>(Notification.EXTRA_DECLINE_INTENT)
            }
        }
        if (answer == null && !incomingByStyle && !looksIncoming(n)) return null

        val name = callerName(n) ?: "người gọi không rõ tên"
        return VoipCallInfo(sbn.key, sbn.packageName, name, answer, decline)
    }

    private val INCOMING_TEXT = listOf(
        "cuộc gọi đến", "gọi đến", "đang gọi cho bạn", "đang gọi bạn", "cuộc gọi thoại đến", "cuộc gọi video đến",
        "incoming voice call", "incoming video call", "incoming call", "calling you",
    )

    /** Thông báo cuộc gọi không có nút Trả lời nhưng nội dung cho thấy đang có cuộc gọi đến. */
    private fun looksIncoming(n: Notification): Boolean {
        val text = TextUtils.normalize(
            listOfNotNull(
                n.extras.getCharSequence(Notification.EXTRA_TITLE),
                n.extras.getCharSequence(Notification.EXTRA_TEXT),
                n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
            ).joinToString(" ")
        )
        return n.fullScreenIntent != null && INCOMING_TEXT.any { text.contains(TextUtils.normalize(it)) }
    }

    /** Cuộc gọi Zalo/WhatsApp đang diễn ra (không còn nút trả lời). */
    fun isOngoingVoipCall(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName !in AppIds.VOIP) return false
        val n = sbn.notification ?: return false
        return isCallNotification(n) && (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
    }

    /** Thông báo cuộc gọi nhỡ của Zalo/WhatsApp. [name]/[number] null nếu thông báo không ghi rõ. */
    data class MissedVoip(val name: String?, val number: String?, val raw: String)

    /** Chữ trong thông báo không phải tên người: "Cuộc gọi nhỡ", "một người", "2 cuộc gọi"... */
    private fun isGenericName(s: String): Boolean {
        val t = TextUtils.normalize(s)
        if (t.isBlank()) return true
        val generic = listOf(
            "cuộc gọi", "gọi nhỡ", "missed", "một người", "1 người", "ai đó", "someone", "người gọi",
            "không xác định", "unknown", "zalo", "whatsapp",
        )
        return generic.any { t.contains(it) } || t.all { it.isDigit() || it == ' ' }
    }

    private val PHONE_RE = Regex("(\\+?84|0)[\\d .\\-]{8,14}\\d")

    fun missedVoipCall(sbn: StatusBarNotification): MissedVoip? {
        if (sbn.packageName !in AppIds.VOIP) return null
        val n = sbn.notification ?: return null
        val title = (n.extras.getCharSequence(Notification.EXTRA_TITLE) ?: "").toString()
        val body = (n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: n.extras.getCharSequence(Notification.EXTRA_TEXT) ?: "").toString()
        val text = TextUtils.normalize("$body $title")
        val isMissed = n.category == CATEGORY_MISSED_CALL ||
            text.contains("cuộc gọi nhỡ") || text.contains("gọi nhỡ") || text.contains("missed voice call") ||
            text.contains("missed video call") || text.contains("missed call")
        if (!isMissed) return null

        // Tên: người trong thông báo kiểu cuộc gọi, tiêu đề, hoặc "… từ <tên>" trong nội dung.
        var name: String? = null
        if (Build.VERSION.SDK_INT >= 31) {
            name = n.extras.parcel<Person>(Notification.EXTRA_CALL_PERSON)?.name?.toString()
        }
        if (name.isNullOrBlank() || isGenericName(name)) name = title.takeIf { !isGenericName(it) }
        if (name == null) {
            Regex("(?:từ|from)\\s+(.+)$", RegexOption.IGNORE_CASE).find(body.trim())?.groupValues?.get(1)
                ?.trim()?.trimEnd('.', '!')?.takeIf { !isGenericName(it) }?.let { name = it }
        }
        // Số điện thoại ghi trong thông báo (nếu có).
        val number = PHONE_RE.find("$title $body")?.value?.filter { it.isDigit() || it == '+' }
        if (name != null && number != null && name!!.filter { it.isDigit() }.length >= 9) name = null
        return MissedVoip(name, number, "$title | $body")
    }

    private fun callerName(n: Notification): String? {
        if (Build.VERSION.SDK_INT >= 31) {
            n.extras.parcel<Person>(Notification.EXTRA_CALL_PERSON)?.name
                ?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }
    }

    // ---------------- Tin nhắn ----------------

    private val lastTimestampByKey = LinkedHashMap<String, Long>()
    private val recentHashes = LinkedHashMap<Int, Long>()

    @Synchronized
    fun newMessages(sbn: StatusBarNotification): List<Pair<StoredMessage, Boolean>> {
        val pkg = sbn.packageName
        if (pkg !in AppIds.VOIP && pkg !in AppIds.SMS) return emptyList()
        val n = sbn.notification ?: return emptyList()
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return emptyList()
        if ((n.flags and Notification.FLAG_ONGOING_EVENT) != 0) return emptyList()
        if (isCallNotification(n) || n.category == CATEGORY_MISSED_CALL) return emptyList()
        if (n.category != null && n.category !in setOf(
                Notification.CATEGORY_MESSAGE, Notification.CATEGORY_SOCIAL, Notification.CATEGORY_EMAIL
            )
        ) return emptyList()

        val app = AppIds.label(pkg)
        val key = (if (Build.VERSION.SDK_INT >= 30) n.shortcutId else null) ?: sbn.key
        val now = System.currentTimeMillis()
        val out = mutableListOf<Pair<StoredMessage, Boolean>>()

        val notifTitle = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val markedGroup = n.extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        if (style != null && style.messages.isNotEmpty()) {
            val rawTitle = style.conversationTitle?.toString()
            val groupTitle = rawTitle?.let { cleanGroupTitle(it) }?.ifBlank { null }
            val me = style.user.name?.toString()
            val senders = style.messages.mapNotNull { it.person?.name?.toString() }
                .filter { it != me }.map { TextUtils.normalize(it) }.distinct()
            // Zalo không đánh dấu tin nhóm (chỉ ghi tiêu đề "Nhóm: …"), nên xét thêm nhiều dấu hiệu.
            val groupBySignals = style.isGroupConversation || markedGroup ||
                looksLikeGroupTitle(rawTitle) || looksLikeGroupTitle(notifTitle) || senders.size >= 2
            val lastTs = lastTimestampByKey[key]
            val candidates = style.messages.filter { m ->
                m.person != null && (m.text?.isNotBlank() == true) &&
                    if (lastTs == null) now - m.timestamp < 3 * 60_000L else m.timestamp > lastTs
            }.takeLast(5)
            style.messages.maxOfOrNull { it.timestamp }?.let { remember(key, it) }
            for (m in candidates) {
                val sender = m.person?.name?.toString() ?: groupTitle ?: "không rõ"
                val text = m.text.toString()
                if (seenRecently(pkg, sender, text, now)) continue
                // Tiêu đề cuộc trò chuyện khác tên người gửi (tiêu đề "hello", người gửi "Thỏ Ngoan") = nhóm.
                val group = groupBySignals || (rawTitle != null && !sameName(rawTitle, sender))
                val senderLabel = if (group && groupTitle != null) "nhóm $groupTitle, $sender" else sender
                out += StoredMessage(app, senderLabel, text, m.timestamp, false, group) to group
            }
            return out
        }

        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return emptyList()
        val text = (n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: n.extras.getCharSequence(Notification.EXTRA_TEXT))?.toString() ?: return emptyList()
        if (text.isBlank()) return emptyList()
        if (seenRecently(pkg, title, text, now)) return emptyList()
        // Thông báo kiểu "5 tin nhắn mới" là tóm tắt, không đọc.
        val norm = TextUtils.normalize(text)
        if (Regex("^\\d+ (tin nhắn mới|new messages)").containsMatchIn(norm)) return emptyList()
        val isGroup = markedGroup || looksLikeGroupTitle(title) ||
            (Regex("^.+: ").containsMatchIn(text) && pkg in AppIds.VOIP)
        val label = if (isGroup && looksLikeGroupTitle(title)) "nhóm ${cleanGroupTitle(title)}" else title
        out += StoredMessage(app, label, text, now, false, isGroup) to isGroup
        return out
    }

    private val GROUP_TITLE = Regex("^\\s*(nhóm|group)\\s*[:：]", RegexOption.IGNORE_CASE)

    /** Tiêu đề kiểu "Nhóm: hello" (Zalo) hoặc "Group: …". */
    private fun looksLikeGroupTitle(title: String?): Boolean = title != null && GROUP_TITLE.containsMatchIn(title)

    /** "Nhóm: hello" → "hello". */
    fun cleanGroupTitle(title: String): String = title.replace(GROUP_TITLE, "").trim()

    /** Hai tên là một người (bỏ dấu câu, emoji; một tên chứa tên kia ở đầu). */
    private fun sameName(a: String, b: String): Boolean {
        val x = TextUtils.normalize(a)
        val y = TextUtils.normalize(b)
        if (x.isBlank() || y.isBlank()) return true
        return x == y || x.startsWith(y) || y.startsWith(x)
    }

    private fun remember(key: String, ts: Long) {
        lastTimestampByKey[key] = maxOf(ts, lastTimestampByKey[key] ?: 0L)
        while (lastTimestampByKey.size > 200) lastTimestampByKey.remove(lastTimestampByKey.keys.first())
    }

    private fun seenRecently(pkg: String, sender: String, text: String, now: Long): Boolean {
        val h = (pkg + "|" + sender + "|" + text).hashCode()
        val prev = recentHashes[h]
        recentHashes[h] = now
        while (recentHashes.size > 300) recentHashes.remove(recentHashes.keys.first())
        return prev != null && now - prev < 30 * 60_000L
    }
}
