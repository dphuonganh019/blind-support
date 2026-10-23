package vn.blindsupport.app.core

import java.text.Normalizer
import java.util.Calendar
import java.util.Locale

object TextUtils {
    private val VI = Locale("vi", "VN")
    private const val PUNCT = ".,;:!?%/-+()'\""

    /** Chữ thường, bỏ ký tự đặc biệt/emoji, gộp khoảng trắng. Dùng để so khớp lời nói. */
    fun normalize(s: String): String {
        val sb = StringBuilder()
        for (ch in Normalizer.normalize(s, Normalizer.Form.NFC).lowercase(VI)) {
            sb.append(if (ch.isLetterOrDigit()) ch else ' ')
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    /** Các từ của câu sau khi chuẩn hóa. */
    fun words(s: String): List<String> = normalize(s).split(' ').filter { it.isNotEmpty() }

    /** Đọc số điện thoại từng chữ số, chia nhóm để nghe dễ: "0 9 1 2, 3 4 5, 6 7 8". */
    fun speakableNumber(number: String?): String {
        if (number.isNullOrBlank()) return "số không xác định"
        var digits = number.filter { it.isDigit() }
        if (number.trim().startsWith("+84") && digits.startsWith("84")) digits = "0" + digits.substring(2)
        if (digits.isEmpty()) return number
        val groups = when (digits.length) {
            10 -> listOf(digits.substring(0, 4), digits.substring(4, 7), digits.substring(7))
            11 -> listOf(digits.substring(0, 4), digits.substring(4, 7), digits.substring(7))
            else -> digits.chunked(3)
        }
        return groups.joinToString(", ") { g -> g.toCharArray().joinToString(" ") }
    }

    private val WEEKDAYS = mapOf(
        Calendar.SUNDAY to "Chủ nhật",
        Calendar.MONDAY to "thứ Hai",
        Calendar.TUESDAY to "thứ Ba",
        Calendar.WEDNESDAY to "thứ Tư",
        Calendar.THURSDAY to "thứ Năm",
        Calendar.FRIDAY to "thứ Sáu",
        Calendar.SATURDAY to "thứ Bảy",
    )

    fun timeSentence(cal: Calendar = Calendar.getInstance()): String {
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val period = when (h) {
            in 0..3 -> "đêm"
            in 4..10 -> "sáng"
            in 11..12 -> "trưa"
            in 13..17 -> "chiều"
            in 18..21 -> "tối"
            else -> "đêm"
        }
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        val minutePart = if (m == 0) "đúng" else "$m phút"
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        val weekday = WEEKDAYS[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
        return "Bây giờ là $h12 giờ $minutePart $period, $weekday, ngày $day tháng $month năm $year."
    }

    /** Thời điểm ngắn gọn cho tin nhắn / cuộc gọi nhỡ: "lúc 9 giờ 15" hoặc "hôm qua lúc ...". */
    fun shortWhen(millis: Long): String {
        val now = Calendar.getInstance()
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        val hm = "${c.get(Calendar.HOUR_OF_DAY)} giờ ${c.get(Calendar.MINUTE)}"
        val sameDay = now.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return "lúc $hm"
        now.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = now.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)
        return if (yesterday) "hôm qua lúc $hm" else "ngày ${c.get(Calendar.DAY_OF_MONTH)} tháng ${c.get(Calendar.MONTH) + 1}"
    }

    private val TOKEN_MAP = listOf(
        Regex("\\[(hình ảnh|ảnh|photo|image)]", RegexOption.IGNORE_CASE) to "một hình ảnh",
        Regex("\\[(sticker|nhãn dán)]", RegexOption.IGNORE_CASE) to "một nhãn dán",
        Regex("\\[(video)]", RegexOption.IGNORE_CASE) to "một video",
        Regex("\\[(tin nhắn thoại|voice|ghi âm|tin nhắn ghi âm)]", RegexOption.IGNORE_CASE) to "một tin nhắn thoại",
        Regex("\\[(file|tệp|tập tin)]", RegexOption.IGNORE_CASE) to "một tệp tin",
        Regex("\\[(link|liên kết)]", RegexOption.IGNORE_CASE) to "một đường link",
        Regex("\\[(danh thiếp|contact)]", RegexOption.IGNORE_CASE) to "một danh thiếp",
        Regex("\\[(vị trí|location)]", RegexOption.IGNORE_CASE) to "một vị trí",
        Regex("(?i)voice message") to "một tin nhắn thoại",
        Regex("(?i)^photo$") to "một hình ảnh",
        Regex("(?i)^sticker$") to "một nhãn dán",
        Regex("https?://\\S+") to "một đường link",
    )

    /** Làm sạch nội dung tin nhắn trước khi đọc: thay ký hiệu bằng lời, bỏ emoji. */
    fun cleanForSpeech(text: String): String {
        var t = Normalizer.normalize(text, Normalizer.Form.NFC).trim()
        for ((re, rep) in TOKEN_MAP) t = t.replace(re, rep)
        val sb = StringBuilder()
        var i = 0
        while (i < t.length) {
            val cp = t.codePointAt(i)
            val type = Character.getType(cp)
            val keep = Character.isLetterOrDigit(cp) || Character.isWhitespace(cp) ||
                type == Character.NON_SPACING_MARK.toInt() ||
                (cp < 128 && cp.toChar() in PUNCT)
            if (keep) sb.appendCodePoint(cp) else sb.append(' ')
            i += Character.charCount(cp)
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    /** Cắt bớt tin quá dài để không đọc mãi. */
    fun limit(text: String, maxChars: Int = 280): String =
        if (text.length <= maxChars) text else text.substring(0, maxChars).substringBeforeLast(' ') + ". Tin nhắn còn dài nữa."
}
