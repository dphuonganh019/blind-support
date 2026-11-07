package vn.blindsupport.app.ui

import vn.blindsupport.app.core.CallIntent
import vn.blindsupport.app.core.Contact
import vn.blindsupport.app.core.DigitWords
import vn.blindsupport.app.core.TextUtils

/** Đang trong phiên nghe lệnh giọng nói (để app không đọc tin nhắn chen ngang). */
object VoiceSession {
    @Volatile var active = false

    /**
     * Gọi khi người dùng bấm nhanh phím giảm âm lượng trong lúc app đang đọc (ví dụ đọc tin nhắn):
     * dừng đọc ngay. Màn hình nghe lệnh gán hàm này khi đang chạy.
     */
    @Volatile var onStopRequested: (() -> Unit)? = null
}

sealed class Command {
    data class CallContact(val contact: Contact) : Command()
    /** "Gọi số ..." – [digits] là phần số đã đọc kèm trong câu lệnh (có thể rỗng). */
    data class CallNumber(val digits: String) : Command()
    object CallBack : Command()
    object ReadMessages : Command()
    object ReadGroupMessages : Command()
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

/** Kết quả bước hỏi lại "đúng hay không" trước khi gọi đi. */
enum class Confirm { YES, NO;

    companion object {
        /** Không còn từ "gọi" đứng một mình: đó là từ rất hay gặp trong câu nói thường và tiếng TV. */
        val YES_WORDS = listOf("đúng", "đúng rồi", "phải", "phải rồi", "vâng", "ừ", "có", "gọi đi", "ok")
        val NO_WORDS = listOf("không", "sai", "không phải", "thôi", "hủy", "không gọi")

        /**
         * Danh sách nghe gồm cả từ "mồi": tiếng ồn, tiếng TV, tiếng người xung quanh bị nhận thành
         * các từ này thay vì thành "đúng".
         */
        val PHRASES: List<String> = (YES_WORDS + NO_WORDS + CallIntent.FILLER_WORDS).distinct()

        private val YES_SET = YES_WORDS.flatMap { TextUtils.words(it) }.toSet()
        private val NO_SET = NO_WORDS.flatMap { TextUtils.words(it) }.toSet()

        /**
         * Cùng luật với câu hỏi "nghe hay không": câu ngắn (tối đa 3 từ), MỌI từ thuộc cùng một
         * nhóm, và có ít nhất một cụm trọn vẹn của nhóm đó. Câu lẫn từ khác bị bỏ qua.
         */
        fun parse(text: String): Confirm? {
            val words = TextUtils.words(text)
            if (words.isEmpty() || words.size > 3) return null
            val t = " " + words.joinToString(" ") + " "
            // "không phải" có chữ "phải": xét "không" trước.
            if (words.all { it in NO_SET } && NO_WORDS.any { t.contains(" $it ") }) return NO
            if (words.all { it in YES_SET } && YES_WORDS.any { t.contains(" $it ") }) return YES
            return null
        }
    }
}

object CommandParser {
    private val FIXED: Map<Command, List<String>> = mapOf(
        Command.CallBack to listOf("gọi lại", "gọi lại số vừa gọi", "gọi lại cho người vừa gọi"),
        Command.ReadGroupMessages to listOf("tin nhóm", "tin nhắn nhóm", "đọc tin nhóm", "đọc tin nhắn nhóm", "nghe tin nhóm"),
        Command.ReadMessages to listOf(
            "đọc tin nhắn", "tin nhắn", "đọc tin", "nghe tin nhắn", "có tin nhắn không", "đọc tin nhắn mới",
            "tin nhắn mới",
        ),
        Command.MissedCalls to listOf("cuộc gọi nhỡ", "gọi nhỡ", "ai gọi", "ai vừa gọi", "có ai gọi không"),
        Command.Time to listOf(
            "mấy giờ", "bây giờ là mấy giờ", "bây giờ mấy giờ", "mấy giờ rồi", "hôm nay ngày bao nhiêu",
            "hôm nay thứ mấy", "thứ mấy", "ngày bao nhiêu",
        ),
        Command.Battery to listOf("pin", "còn bao nhiêu pin", "kiểm tra pin", "pin còn bao nhiêu", "báo pin", "còn pin không"),
        Command.VolumeUp to listOf("to lên", "to hơn", "nói to", "nói to lên", "tăng âm lượng"),
        Command.VolumeDown to listOf("nhỏ xuống", "nhỏ hơn", "nói nhỏ", "nói nhỏ lại", "giảm âm lượng"),
        Command.SpeakSlower to listOf("nói chậm", "nói chậm lại", "chậm lại", "chậm hơn"),
        Command.SpeakFaster to listOf("nói nhanh", "nói nhanh lên", "nhanh lên", "nhanh hơn"),
        Command.Help to listOf("hướng dẫn", "trợ giúp", "giúp tôi"),
        Command.ReadContacts to listOf("danh bạ", "đọc danh bạ"),
        Command.Cancel to listOf("thôi", "hủy", "không", "không có gì", "tạm biệt"),
    )

    /** Từ đệm hay nói ở đầu câu ("à, gọi mẹ") – được bỏ đi trước khi so khớp. */
    private val LEADING = setOf("à", "ờ", "ừ", "ừm", "này", "ơi", "thì")
    /** Từ đệm hay nói ở cuối câu ("gọi mẹ đi", "đọc tin nhắn nhé") – được bỏ đi trước khi so khớp. */
    private val TRAILING = setOf("đi", "nhé", "nha", "ạ", "à", "ơi", "với", "nhỉ", "giúp", "giùm", "cái")

    fun phrases(contacts: List<Contact>): List<String> {
        val list = FIXED.values.flatten().toMutableList()
        // "gọi số" kèm chữ số: bộ nhận dạng cho phép nối các cụm, nên liệt kê từng chữ số riêng.
        list += listOf("gọi số", "gọi đến số", "bấm số")
        list += DigitWords.WORDS
        for (c in contacts) {
            list += "gọi ${c.normalizedName}"
            list += "gọi cho ${c.normalizedName}"
        }
        // Từ mồi: tiếng TV, tiếng người xung quanh bị nhận thành các từ này thay vì thành một lệnh.
        list += CallIntent.FILLER_WORDS
        list += LEADING + TRAILING
        return list.distinct()
    }

    /** Bỏ từ đệm ở hai đầu câu. Không bao giờ bỏ hết cả câu. */
    fun stripFillers(words: List<String>): List<String> {
        var w = words
        while (w.size > 1 && w.first() in LEADING) w = w.drop(1)
        while (w.size > 1 && w.last() in TRAILING) w = w.dropLast(1)
        return w
    }

    /**
     * Chỉ nhận khi CẢ CÂU (sau khi bỏ từ đệm ở hai đầu) đúng là một lệnh. Câu dài chỉ "có chứa"
     * một cụm lệnh (thường là tiếng TV, người xung quanh nói chuyện) bị bỏ qua.
     */
    fun parse(text: String, contacts: List<Contact>): Command? {
        val words = stripFillers(TextUtils.words(text))
        if (words.isEmpty()) return null
        val t = words.joinToString(" ")
        for (prefix in listOf("gọi đến số", "gọi số", "bấm số")) {
            if (t == prefix || t.startsWith("$prefix ")) {
                val rest = TextUtils.words(t.removePrefix(prefix))
                // Phần sau "gọi số" phải toàn là chữ số; lẫn từ khác thì không nhận.
                if (rest.any { w -> DigitWords.toDigits(w).isEmpty() }) return null
                return Command.CallNumber(DigitWords.toDigits(rest.joinToString(" ")))
            }
        }
        for ((cmd, list) in FIXED) {
            if (list.any { it == t }) return cmd
        }
        if (t.startsWith("gọi ")) {
            val rest = t.removePrefix("gọi ").let { if (it.startsWith("cho ")) it.removePrefix("cho ") else it }.trim()
            return matchContact(rest, contacts)?.let { Command.CallContact(it) }
        }
        return null
    }

    /**
     * Tìm người trong danh bạ theo phần tên đã nói. Nhận khi nói đúng cả tên, hoặc nói một phần
     * tên (ví dụ "Hưng" cho "Anh Hưng") mà không bị nhầm với người khác.
     */
    fun matchContact(spoken: String, contacts: List<Contact>): Contact? {
        if (spoken.isBlank()) return null
        contacts.firstOrNull { it.normalizedName == spoken }?.let { return it }
        val partial = contacts.filter { " ${it.normalizedName} ".contains(" $spoken ") }
        return partial.singleOrNull()
    }

    fun isYes(text: String): Boolean = Confirm.parse(text) == Confirm.YES

    fun isNo(text: String): Boolean = Confirm.parse(text) == Confirm.NO

    const val HELP =
        "Các lệnh bạn có thể nói: Gọi, kèm tên trong danh bạ, ví dụ: gọi Mẹ. " +
            "Gọi số, rồi đọc từng số điện thoại. " +
            "Gọi lại, để gọi lại người vừa gọi. Đọc tin nhắn. Tin nhóm, để nghe tin nhắn nhóm. " +
            "Cuộc gọi nhỡ. Mấy giờ. Pin. " +
            "Danh bạ, để nghe tên trong danh bạ. To lên, hoặc nhỏ xuống, để chỉnh âm lượng giọng đọc. " +
            "Nói chậm, hoặc nói nhanh. Khi có cuộc gọi đến, nói: nghe, hoặc: không. " +
            "Bạn cũng có thể bấm phím tăng âm lượng để nghe máy. Phím giảm âm lượng chỉ để tắt chuông, " +
            "không từ chối cuộc gọi. Khi đang đọc tin nhắn, bấm phím giảm âm lượng để dừng đọc."
}
