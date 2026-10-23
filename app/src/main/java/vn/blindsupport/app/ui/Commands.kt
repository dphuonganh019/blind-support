package vn.blindsupport.app.ui

import vn.blindsupport.app.core.Contact
import vn.blindsupport.app.core.TextUtils

/** Đang trong phiên nghe lệnh giọng nói (để app không đọc tin nhắn chen ngang). */
object VoiceSession {
    @Volatile var active = false
}

sealed class Command {
    data class CallContact(val contact: Contact) : Command()
    object CallBack : Command()
    object ReadMessages : Command()
    object MissedCalls : Command()
    object Time : Command()
    object Battery : Command()
    object VolumeUp : Command()
    object VolumeDown : Command()
    object SpeakSlower : Command()
    object SpeakFaster : Command()
    object Help : Command()
    object ReadContacts : Command()
    object Cancel : Command()
}

object CommandParser {
    private val FIXED: Map<Command, List<String>> = mapOf(
        Command.CallBack to listOf("gọi lại", "gọi lại số vừa gọi"),
        Command.ReadMessages to listOf("đọc tin nhắn", "tin nhắn", "đọc tin", "nghe tin nhắn", "có tin nhắn không"),
        Command.MissedCalls to listOf("cuộc gọi nhỡ", "gọi nhỡ", "ai gọi", "ai vừa gọi"),
        Command.Time to listOf("mấy giờ", "giờ", "bây giờ là mấy giờ", "mấy giờ rồi", "hôm nay ngày bao nhiêu", "ngày", "hôm nay thứ mấy", "thứ mấy"),
        Command.Battery to listOf("pin", "còn bao nhiêu pin", "kiểm tra pin", "pin còn bao nhiêu"),
        Command.VolumeUp to listOf("to lên", "to hơn", "nói to", "nói to lên", "tăng âm lượng"),
        Command.VolumeDown to listOf("nhỏ xuống", "nhỏ hơn", "nói nhỏ", "nói nhỏ lại", "giảm âm lượng"),
        Command.SpeakSlower to listOf("nói chậm", "nói chậm lại", "chậm lại", "chậm hơn"),
        Command.SpeakFaster to listOf("nói nhanh", "nói nhanh lên", "nhanh lên", "nhanh hơn"),
        Command.Help to listOf("hướng dẫn", "giúp", "trợ giúp", "giúp tôi", "lệnh"),
        Command.ReadContacts to listOf("danh bạ", "đọc danh bạ"),
        Command.Cancel to listOf("thôi", "hủy", "không", "không có gì", "tạm biệt"),
    )

    val CONFIRM_YES = listOf("đúng", "đúng rồi", "phải", "có", "ừ", "vâng", "gọi", "gọi đi", "ok")
    val CONFIRM_NO = listOf("không", "sai", "không phải", "thôi", "hủy")

    fun phrases(contacts: List<Contact>): List<String> {
        val list = FIXED.values.flatten().toMutableList()
        for (c in contacts) {
            list += "gọi ${c.normalizedName}"
            list += "gọi cho ${c.normalizedName}"
        }
        return list
    }

    fun parse(text: String, contacts: List<Contact>): Command? {
        val t = TextUtils.normalize(text)
        if (t.isEmpty()) return null
        // "gọi lại" phải kiểm tra trước "gọi <tên>".
        if (t == "gọi lại" || t.startsWith("gọi lại ")) return Command.CallBack
        if (t.startsWith("gọi ")) {
            val rest = t.removePrefix("gọi ").removePrefix("cho ").trim()
            matchContact(rest, contacts)?.let { return Command.CallContact(it) }
        }
        // Khớp cụm cố định: ưu tiên cụm dài nhất có mặt trong câu.
        var best: Pair<Command, Int>? = null
        for ((cmd, list) in FIXED) {
            for (p in list) {
                if (" $t ".contains(" $p ")) {
                    if (best == null || p.length > best.second) best = cmd to p.length
                }
            }
        }
        return best?.first
    }

    fun matchContact(spoken: String, contacts: List<Contact>): Contact? {
        if (spoken.isBlank()) return null
        contacts.firstOrNull { it.normalizedName == spoken }?.let { return it }
        return contacts
            .filter { spoken.contains(it.normalizedName) || it.normalizedName.contains(spoken) }
            .maxByOrNull { it.normalizedName.length }
    }

    fun isYes(text: String): Boolean {
        val t = " " + TextUtils.normalize(text) + " "
        if (CONFIRM_NO.any { t.contains(" $it ") }) return false
        return CONFIRM_YES.any { t.contains(" $it ") }
    }

    fun isNo(text: String): Boolean {
        val t = " " + TextUtils.normalize(text) + " "
        return CONFIRM_NO.any { t.contains(" $it ") }
    }

    const val HELP =
        "Các lệnh bạn có thể nói: Gọi, kèm tên trong danh bạ, ví dụ: gọi Mẹ. " +
            "Gọi lại, để gọi lại người vừa gọi. Đọc tin nhắn. Cuộc gọi nhỡ. Mấy giờ. Pin. " +
            "Danh bạ, để nghe tên trong danh bạ. To lên, hoặc nhỏ xuống, để chỉnh âm lượng giọng đọc. " +
            "Nói chậm, hoặc nói nhanh. Khi có cuộc gọi đến, nói: nghe, hoặc: không. " +
            "Bạn cũng có thể bấm phím tăng âm lượng để nghe, phím giảm âm lượng để từ chối."
}
