package vn.blindsupport.app.notify

import vn.blindsupport.app.core.TextUtils

/**
 * Nhận ra cuộc gọi Zalo đang đổ chuông từ CHỮ trên màn hình hoặc trong thông báo.
 *
 * Dùng cho Android 11 trở xuống: Zalo trên các máy này (ví dụ OPPO A92) không dùng kiểu thông báo
 * cuộc gọi chuẩn của Android 12+, nên cách nhận cũ không thấy cuộc gọi.
 */
object CallScreenText {
    private val INCOMING = listOf("cuộc gọi thoại đến", "cuộc gọi video đến", "cuộc gọi đến")
        .map { TextUtils.normalize(it) }
    private val MISSED = listOf("nhỡ", "missed").map { TextUtils.normalize(it) }

    private fun padded(s: String) = " " + TextUtils.normalize(s) + " "

    /** Chữ cho biết đang có cuộc gọi Zalo đến (không phải cuộc gọi nhỡ). */
    fun isIncomingText(s: String): Boolean {
        val t = padded(s)
        if (MISSED.any { t.contains(" $it ") }) return false
        return INCOMING.any { t.contains(" $it ") }
    }

    /**
     * Các dòng chữ trên màn hình Zalo (theo thứ tự từ trên xuống) có phải màn hình cuộc gọi đến
     * không: phải có chữ "Cuộc gọi … đến" VÀ cả hai nút "Trả lời", "Từ chối". Màn hình trò chuyện
     * cũng có thể có dòng "Cuộc gọi thoại đến" (lịch sử cuộc gọi) nhưng không có hai nút đó.
     * Trả về tên người gọi ("" nếu không đọc được tên), hoặc null nếu không phải.
     */
    fun incomingCaller(texts: List<String>): String? {
        val idx = texts.indexOfFirst { isIncomingText(it) }
        if (idx < 0) return null
        val hasAnswer = texts.any { padded(it).contains(" trả lời ") }
        val hasDecline = texts.any { padded(it).contains(" từ chối ") }
        if (!hasAnswer || !hasDecline) return null
        // Tên người gọi: dòng chữ ngay phía trên dòng "Cuộc gọi … đến".
        for (i in idx - 1 downTo 0) {
            val name = cleanName(texts[i]) ?: continue
            return name
        }
        // Hoặc nằm cùng dòng: "Con Gái: Cuộc gọi thoại đến".
        return nameFromLine(texts[idx]) ?: ""
    }

    /** Tên người gọi từ tiêu đề / nội dung thông báo cuộc gọi Zalo. */
    fun callerFromNotification(title: String?, text: String?): String? {
        title?.let { t -> if (!isIncomingText(t)) cleanName(t)?.let { return it } }
        title?.let { t -> nameFromLine(t)?.let { return it } }
        text?.let { t -> if (!isIncomingText(t)) cleanName(t)?.let { return it } }
        text?.let { t -> nameFromLine(t)?.let { return it } }
        return null
    }

    /** "Con Gái: Cuộc gọi thoại đến" → "Con Gái". */
    private fun nameFromLine(line: String): String? {
        val before = line.substringBefore(':', "").trim()
        if (before.isEmpty()) return null
        return cleanName(before)
    }

    private fun cleanName(raw: String): String? {
        val s = raw.trim()
        if (s.isEmpty() || s.length > 40) return null
        val n = TextUtils.normalize(s)
        if (n.isEmpty() || n == "zalo") return null
        if (Regex("^[0-9 ]+$").matches(n)) return null // giờ "15 50", số phần trăm pin...
        if (isIncomingText(s) || n.contains("cuộc gọi")) return null
        if (n == "trả lời" || n == "từ chối") return null
        return s
    }
}
