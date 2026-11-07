package vn.blindsupport.app.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class StoredMessage(
    val app: String,
    val sender: String,
    val text: String,
    val time: Long,
    var read: Boolean,
    /** Tin trong nhóm chat: lưu riêng, không lẫn vào "đọc tin nhắn". */
    val group: Boolean = false,
)

/**
 * Lưu tin nhắn gần nhất để người dùng nghe lại: 40 tin cá nhân ("đọc tin nhắn") và 40 tin nhóm
 * ("tin nhóm") giữ riêng, để tin nhóm dồn dập không đẩy mất tin của con cháu.
 */
object MessageStore {
    private const val MAX = 40
    private const val MAX_GROUP = 40
    private val items = ArrayList<StoredMessage>()
    private var loaded = false

    @Synchronized
    fun init(context: Context) {
        if (loaded) return
        loaded = true
        runCatching {
            val arr = JSONArray(Prefs.messagesJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items += StoredMessage(
                    o.optString("app"), o.optString("sender"), o.optString("text"),
                    o.optLong("time"), o.optBoolean("read"), o.optBoolean("group", o.optString("sender").startsWith("nhóm ")) // tin lưu từ bản cũ
                )
            }
        }
    }

    @Synchronized
    fun add(msg: StoredMessage) {
        items += msg
        val cap = if (msg.group) MAX_GROUP else MAX
        while (items.count { it.group == msg.group } > cap) {
            items.removeAt(items.indexOfFirst { it.group == msg.group })
        }
        save()
    }

    /** Tin cá nhân chưa nghe (không gồm tin nhóm). */
    @Synchronized
    fun unread(): List<StoredMessage> = items.filter { !it.read && !it.group }

    @Synchronized
    fun unreadGroups(): List<StoredMessage> = items.filter { !it.read && it.group }

    /** Các tin cá nhân gần nhất. */
    @Synchronized
    fun latest(n: Int): List<StoredMessage> = items.filter { !it.group }.takeLast(n)

    @Synchronized
    fun unreadCount(): Int = items.count { !it.read && !it.group }

    @Synchronized
    fun unreadGroupCount(): Int = items.count { !it.read && it.group }

    @Synchronized
    fun markRead(list: Collection<StoredMessage>) {
        list.forEach { it.read = true }
        save()
    }

    @Synchronized
    fun markAllRead() {
        items.forEach { it.read = true }
        save()
    }

    private fun save() {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("app", it.app).put("sender", it.sender).put("text", it.text)
                    .put("time", it.time).put("read", it.read).put("group", it.group)
            )
        }
        Prefs.messagesJson = arr.toString()
    }
}
