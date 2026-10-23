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
)

/** Lưu khoảng 40 tin nhắn gần nhất để người dùng nói "đọc tin nhắn" nghe lại. */
object MessageStore {
    private const val MAX = 40
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
                    o.optLong("time"), o.optBoolean("read")
                )
            }
        }
    }

    @Synchronized
    fun add(msg: StoredMessage) {
        items += msg
        while (items.size > MAX) items.removeAt(0)
        save()
    }

    @Synchronized
    fun unread(): List<StoredMessage> = items.filter { !it.read }

    @Synchronized
    fun latest(n: Int): List<StoredMessage> = items.takeLast(n)

    @Synchronized
    fun unreadCount(): Int = items.count { !it.read }

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
                    .put("time", it.time).put("read", it.read)
            )
        }
        Prefs.messagesJson = arr.toString()
    }
}
