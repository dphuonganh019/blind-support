package vn.blindsupport.app.ui

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
    /** Tìm được đúng một người trong danh bạ. */
    data class CallContact(val contact: Contact) : Command()
    /** Nhiều người khớp phần tên đã nói: đọc danh sách có số thứ tự cho người dùng chọn. */
    data class ChooseContact(val spoken: String, val candidates: List<Contact>) : Command()
    /** Quá nhiều người khớp (ví dụ chỉ nói "anh"): cần nói thêm tên. */
    data class TooManyContacts(val spoken: String, val count: Int) : Command()
    /** Nói tên nhưng không có ai trong danh bạ khớp. */
    data class ContactNotFound(val spoken: String) : Command()
    /** Chỉ nói "gọi": hỏi gọi ai. */
    object CallWho : Command()
    /**
     * "Gọi Zalo / WhatsApp …": tìm người như lệnh gọi thường ([inner]), rồi gọi qua [app]. Không gọi
     * qua [app] được thì báo và gọi bằng sim (vẫn có bước hỏi "đúng không" trước khi gọi).
     */
    data class AppCall(val app: String, val inner: Command) : Command()
    /** "Gọi" + số điện thoại – [digits] là phần số đã đọc kèm trong câu lệnh (có thể rỗng). */
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
        val YES_WORDS = listOf(
            "đúng", "đúng rồi", "phải", "phải rồi", "vâng", "ừ", "có", "gọi đi", "ok",
            // Bộ nhận dạng Google hay nghe "đúng" thành các chữ này (đã gặp trên máy thật).
            "đốm", "đúm", "đùng", "đung",
        )
        val NO_WORDS = listOf("không", "sai", "không phải", "thôi", "hủy", "không gọi")

        /**
         * Vài từ "mồi" khác âm hẳn với "đúng"/"không": tiếng ồn, tiếng TV bị nhận thành các từ này
         * thay vì thành "đúng". Chỉ dùng ít từ, tránh các từ gần âm (đó, đi, được...) để không làm
         * lời nói thật bị nghe sai.
         */
        private val DECOYS = listOf(
            "à", "ờ", "ừm", "hả", "gì", "này", "nào", "ai", "ăn", "về", "làm", "người", "nhà",
            "ở", "ra", "vào", "biết", "thấy", "nói", "tiếng", "chuông",
        )
        val PHRASES: List<String> = (YES_WORDS + NO_WORDS + DECOYS).distinct()

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

/** Chọn một người trong danh sách đã đọc bằng số thứ tự ("hai", "số hai", "người thứ hai"). */
sealed class Choice {
    data class Pick(val index: Int) : Choice()
    object Cancel : Choice()

    companion object {
        private val SKIP = setOf("số", "thứ", "người", "tự", "gọi", "à", "ạ", "nhé", "đi")
        private val CANCEL = setOf("hủy", "thôi", "không")

        fun phrases(): List<String> =
            DigitWords.WORDS + listOf("số", "thứ", "người thứ", "số thứ tự", "nhất", "hủy", "thôi", "không gọi")

        /** [count]: số người trong danh sách. Trả về null nếu không nghe ra lựa chọn hợp lệ. */
        fun parse(text: String, count: Int): Choice? {
            val all = TextUtils.words(text)
            if (all.isEmpty() || all.size > 4) return null
            val words = all.filter { it !in SKIP }
            // "không" đứng một mình (hoặc "không gọi", "thôi") là hủy, không phải số 0.
            if (words.isNotEmpty() && words.all { it in CANCEL || it == "ai" }) return Cancel
            if (words.size != 1) return null
            val n = if (words[0] == "nhất") 1 else DigitWords.toDigits(words[0]).singleOrNull()?.digitToInt()
            return if (n != null && n in 1..count) Pick(n - 1) else null
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

    /** Danh sách chọn tối đa chừng này người; nhiều hơn thì hỏi nói thêm tên. */
    const val MAX_CHOICES = 6

    /** Từ đệm hay nói ở đầu câu ("à, gọi mẹ") – được bỏ đi trước khi so khớp. */
    private val LEADING = setOf("à", "ờ", "ừ", "ừm", "này", "ơi", "thì")
    /** Từ đệm hay nói ở cuối câu ("gọi mẹ đi", "đọc tin nhắn nhé") – được bỏ đi trước khi so khớp. */
    private val TRAILING = setOf("đi", "nhé", "nha", "ạ", "à", "ơi", "với", "nhỉ", "giúp", "giùm", "cái")
    /** Hư từ không mang nghĩa lệnh, được bỏ ở bất kỳ đâu trong câu ("đọc tin nhắn à nhé"). */
    private val PARTICLES = setOf("à", "ạ", "ờ", "ừm", "ơi", "nhé", "nha", "nhỉ", "hả")
    /** Các chữ có thể đứng sau "gọi" trước tên hoặc số: "gọi điện cho", "gọi đến số"... */
    private val CALL_HEAD = setOf("gọi", "điện", "cho", "đến", "tới", "số", "bấm")

    /**
     * Danh sách cụm cho bộ nhận dạng. Bộ nhận dạng cho phép nối các cụm với nhau, nên chỉ cần
     * liệt kê từng chữ của tên danh bạ và từng chữ số – người dùng nói một phần tên hay đọc
     * số điện thoại dài bao nhiêu cũng nghe được.
     *
     * KHÔNG đưa "từ mồi" (ăn, nam, mô, người...) vào đây: chúng làm tên và chữ số bị nghe nhầm
     * (ví dụ "năm" thành "nam"), khiến lệnh gọi không dùng được.
     */
    fun phrases(contacts: List<Contact>): List<String> {
        val list = FIXED.values.flatten().toMutableList()
        list += listOf("gọi", "gọi cho", "gọi điện", "gọi điện cho", "gọi số", "gọi đến số", "bấm số", "số")
        list += DigitWords.WORDS
        list += namePhrases(contacts)
        list += LEADING + TRAILING
        return list.distinct()
    }

    /** Tên danh bạ (đầy đủ và từng chữ), để nghe khi hỏi "gọi ai". */
    fun namePhrases(contacts: List<Contact>): List<String> {
        val list = ArrayList<String>()
        for (c in contacts) {
            if (c.normalizedName.isBlank()) continue
            list += c.normalizedName
            list += TextUtils.words(c.normalizedName)
        }
        return list.distinct()
    }

    /**
     * Bỏ từ đệm ở hai đầu câu, và tối đa 2 hư từ ở giữa câu. Không bao giờ bỏ hết cả câu.
     */
    fun stripFillers(words: List<String>): List<String> {
        var w = words
        while (w.size > 1 && w.first() in LEADING) w = w.drop(1)
        while (w.size > 1 && w.last() in TRAILING) w = w.dropLast(1)
        val inner = w.count { it in PARTICLES }
        if (inner in 1..2 && w.size - inner >= 1) w = w.filter { it !in PARTICLES }
        return w
    }

    fun parse(text: String, contacts: List<Contact>): Command? {
        val words = stripFillers(TextUtils.words(text))
        if (words.isEmpty()) return null
        val t = words.joinToString(" ")

        if (t == "gọi lại" || (t.startsWith("gọi lại ") && words.size <= 5)) return Command.CallBack
        if (words.first() == "gọi" || t.startsWith("bấm số")) return parseCall(words, contacts)

        // Lệnh cố định: khớp đúng cả câu, hoặc câu chứa cụm lệnh và chỉ dài hơn tối đa 2 chữ.
        // (Câu dài hơn thường là tiếng TV / người xung quanh nói chuyện.)
        for ((cmd, list) in FIXED) {
            if (list.any { it == t }) return cmd
        }
        var best: Pair<Command, Int>? = null
        for ((cmd, list) in FIXED) {
            for (p in list) {
                val pw = p.split(' ').size
                if (words.size <= pw + 2 && " $t ".contains(" $p ")) {
                    if (best == null || p.length > best.second) best = cmd to p.length
                }
            }
        }
        return best?.first
    }

    /** "gọi …": phân biệt gọi theo số điện thoại hay theo tên danh bạ. */
    private val APP_WORDS = mapOf(
        "zalo" to "Zalo", "da lô" to "Zalo", "za lô" to "Zalo", "dalo" to "Zalo",
        "whatsapp" to "WhatsApp", "oát sáp" to "WhatsApp", "wát sáp" to "WhatsApp", "what" to "WhatsApp",
    )

    private fun parseCall(words: List<String>, contacts: List<Contact>): Command {
        // "gọi zalo chị Trang", "gọi chị Trang qua zalo": tách tên ứng dụng ra.
        val joined = " " + words.joinToString(" ") + " "
        val app = APP_WORDS.entries.firstOrNull { joined.contains(" ${it.key} ") }
        if (app != null) {
            val cleaned = joined.replace(" ${app.key} ", " ").split(' ')
                .filter { it.isNotEmpty() && it != "qua" && it != "bằng" && it !in APP_WORDS.keys && it != "sáp" }
            return Command.AppCall(app.value, parseCallPlain(cleaned, contacts))
        }
        return parseCallPlain(words, contacts)
    }

    private fun parseCallPlain(words: List<String>, contacts: List<Contact>): Command {
        var rest = words
        var saidNumber = false
        while (rest.isNotEmpty() && rest.first() in CALL_HEAD) {
            if (rest.first() == "số") saidNumber = true
            rest = rest.drop(1)
        }
        if (rest.isEmpty()) return if (saidNumber) Command.CallNumber("") else Command.CallWho

        val perWord = rest.map { DigitWords.toDigits(it) }
        val digits = perWord.joinToString("")
        val nonDigit = perWord.count { it.isEmpty() }
        // Số điện thoại: toàn chữ số (cho phép lẫn 1 chữ nghe nhầm nếu đã đọc từ 3 số trở lên).
        if ((digits.length >= 3 && nonDigit <= 1) || (saidNumber && nonDigit == 0)) {
            return Command.CallNumber(digits)
        }
        if (rest == listOf("không")) return Command.Cancel

        val spoken = rest.joinToString(" ")
        val found = findContacts(rest, contacts)
        return when {
            found.isEmpty() -> Command.ContactNotFound(spoken)
            found.size == 1 -> Command.CallContact(found[0])
            found.size > MAX_CHOICES -> Command.TooManyContacts(spoken, found.size)
            else -> Command.ChooseContact(spoken, found)
        }
    }

    /**
     * Tìm người trong danh bạ theo phần tên đã nói (không phân biệt dấu, không cần đúng thứ tự):
     *  1. Người có tên chứa TẤT CẢ các chữ đã nói. Nếu đúng một người có tên trùng hẳn câu nói
     *     thì chọn luôn người đó.
     *  2. Không ai chứa đủ: lấy những người chứa NHIỀU chữ nhất (ví dụ nói "chị Trang" mà danh bạ
     *     lưu "Trang Nguyễn").
     */
    fun findContacts(spokenWords: List<String>, contacts: List<Contact>): List<Contact> {
        val spoken = spokenWords.map { TextUtils.fold(it) }.filter { it.isNotEmpty() }
        if (spoken.isEmpty()) return emptyList()
        val folded = contacts.map { it to TextUtils.words(it.normalizedName).map { w -> TextUtils.fold(w) } }
            .filter { it.second.isNotEmpty() }

        val exact = folded.filter { it.second == spoken }
        if (exact.size == 1) return listOf(exact[0].first)

        val all = folded.filter { (_, nameWords) -> spoken.all { it in nameWords } }
        if (all.isNotEmpty()) return sortCandidates(all.map { it.first }, spoken)

        val scored = folded.map { (c, nameWords) -> c to spoken.count { it in nameWords } }.filter { it.second > 0 }
        val top = scored.maxOfOrNull { it.second } ?: return emptyList()
        return sortCandidates(scored.filter { it.second == top }.map { it.first }, spoken)
    }

    /** Tên ngắn hơn (khớp sát hơn) lên trước, rồi theo thứ tự chữ cái. */
    /**
     * Tên ngắn hơn (khớp sát hơn) lên trước, rồi theo thứ tự chữ cái. Cùng một số điện thoại chỉ
     * giữ một người – ưu tiên tên lưu trong danh bạ máy hơn tên Zalo tự đồng bộ ("Zalo …").
     */
    private fun sortCandidates(list: List<Contact>, spoken: List<String>): List<Contact> =
        list.sortedWith(
            compareBy<Contact>(
                { if (it.normalizedName.startsWith("zalo ")) 1 else 0 },
                { TextUtils.words(it.normalizedName).size - spoken.size },
                { it.normalizedName },
            )
        ).distinctBy { TextUtils.dialableNumber(it.number) }

    fun isYes(text: String): Boolean = Confirm.parse(text) == Confirm.YES

    fun isNo(text: String): Boolean = Confirm.parse(text) == Confirm.NO

    const val HELP =
        "Các lệnh bạn có thể nói: Gọi, kèm tên trong danh bạ, ví dụ: gọi Mẹ. Có thể nói một phần tên; " +
            "nếu nhiều người trùng tên, tôi sẽ đọc danh sách để bạn chọn bằng số thứ tự. " +
            "Gọi số, kèm số điện thoại, ví dụ: gọi số không chín một hai. " +
            "Gọi Zalo, hoặc gọi WhatsApp, kèm tên, để gọi qua Zalo hay WhatsApp. " +
            "Gọi lại, để gọi lại người vừa gọi. Đọc tin nhắn. Tin nhóm, để nghe tin nhắn nhóm. " +
            "Cuộc gọi nhỡ. Mấy giờ. Pin. " +
            "Danh bạ, để nghe tên trong danh bạ. To lên, hoặc nhỏ xuống, để chỉnh âm lượng giọng đọc. " +
            "Nói chậm, hoặc nói nhanh. Khi có cuộc gọi đến, nói: nghe, hoặc: không. " +
            "Hoặc bấm phím bất kỳ ở cạnh máy để nghe máy. " +
            "Khi đang đọc tin nhắn, bấm phím giảm âm lượng để dừng đọc."
}
