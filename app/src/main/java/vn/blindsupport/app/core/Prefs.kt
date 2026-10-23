package vn.blindsupport.app.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Các cài đặt của app. Giá trị mặc định được chọn để máy chạy ổn khi không có ai chỉnh lại.
 */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        if (!::sp.isInitialized) {
            sp = context.applicationContext.getSharedPreferences("blind_support", Context.MODE_PRIVATE)
        }
    }

    private fun bool(key: String, def: Boolean) = sp.getBoolean(key, def)
    private fun setBool(key: String, v: Boolean) = sp.edit().putBoolean(key, v).apply()

    // Tin nhắn
    var readZalo: Boolean
        get() = bool("read_zalo", true)
        set(v) = setBool("read_zalo", v)
    var readWhatsapp: Boolean
        get() = bool("read_whatsapp", true)
        set(v) = setBool("read_whatsapp", v)
    var readSms: Boolean
        get() = bool("read_sms", true)
        set(v) = setBool("read_sms", v)
    /** Nhóm Zalo thường rất nhiều tin, mặc định chỉ lưu lại chứ không đọc to. */
    var readGroups: Boolean
        get() = bool("read_groups", false)
        set(v) = setBool("read_groups", v)
    /** Đọc cả nội dung tin hay chỉ đọc tên người gửi. */
    var readContent: Boolean
        get() = bool("read_content", true)
        set(v) = setBool("read_content", v)

    // Cuộc gọi
    var voipCalls: Boolean
        get() = bool("voip_calls", true)
        set(v) = setBool("voip_calls", v)
    var autoSpeaker: Boolean
        get() = bool("auto_speaker", true)
        set(v) = setBool("auto_speaker", v)
    var voiceAnswer: Boolean
        get() = bool("voice_answer", true)
        set(v) = setBool("voice_answer", v)

    // Giờ yên tĩnh: không đọc tin nhắn, vẫn báo cuộc gọi
    var quietHours: Boolean
        get() = bool("quiet_hours", true)
        set(v) = setBool("quiet_hours", v)
    var quietStart: Int
        get() = sp.getInt("quiet_start", 22)
        set(v) = sp.edit().putInt("quiet_start", v).apply()
    var quietEnd: Int
        get() = sp.getInt("quiet_end", 6)
        set(v) = sp.edit().putInt("quiet_end", v).apply()

    var announceBattery: Boolean
        get() = bool("announce_battery", true)
        set(v) = setBool("announce_battery", v)

    var speechRate: Float
        get() = sp.getFloat("speech_rate", 0.95f)
        set(v) = sp.edit().putFloat("speech_rate", v).apply()

    /** Âm lượng chuông gốc khi app tạm hạ chuông; -1 nghĩa là không có gì cần trả lại. */
    var savedRingVolume: Int
        get() = sp.getInt("saved_ring_volume", -1)
        set(v) = sp.edit().putInt("saved_ring_volume", v).apply()
    var savedRingTime: Long
        get() = sp.getLong("saved_ring_time", 0L)
        set(v) = sp.edit().putLong("saved_ring_time", v).apply()

    var lastMissedNumber: String?
        get() = sp.getString("last_missed_number", null)
        set(v) = sp.edit().putString("last_missed_number", v).apply()
    var lastMissedName: String?
        get() = sp.getString("last_missed_name", null)
        set(v) = sp.edit().putString("last_missed_name", v).apply()

    var messagesJson: String
        get() = sp.getString("messages_json", "[]") ?: "[]"
        set(v) = sp.edit().putString("messages_json", v).apply()

    var lastBatteryLevelAnnounced: Int
        get() = sp.getInt("last_battery_announced", 100)
        set(v) = sp.edit().putInt("last_battery_announced", v).apply()

    fun isQuietNow(hour: Int): Boolean {
        if (!quietHours) return false
        val s = quietStart
        val e = quietEnd
        return if (s == e) false else if (s < e) hour in s until e else hour >= s || hour < e
    }
}
