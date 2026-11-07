package vn.blindsupport.app.a11y

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.blindsupport.app.App
import vn.blindsupport.app.core.Prefs

/**
 * "Hẹn" dịch vụ trợ năng bấm một nút trong Zalo/WhatsApp trong một khoảng thời gian ngắn
 * (ví dụ nút Loa ngay sau khi nghe máy). Ngoài khoảng thời gian đó app không đụng vào Zalo.
 */
object UiClicker {
    // "speakerbtn": mã nút Loa của Zalo (call_speakerBtn), tìm theo mã trước.
    val SPEAKER_KEYS = listOf("speakerbtn", "loa", "loa ngoài", "speaker", "bật loa", "speakerphone", "loudspeaker", "spk")
    val ANSWER_KEYS = listOf("trả lời", "nghe", "answer", "accept", "chấp nhận")
    val DECLINE_KEYS = listOf("từ chối", "decline", "reject", "kết thúc", "end call", "hang up")

    class Task(
        val pkg: String,
        val keywords: List<String>,
        @Volatile var deadline: Long,
        val onDone: (Boolean) -> Unit,
    ) {
        @Volatile var finished = false
        /** Đã xin mở lại màn hình của ứng dụng (khi không thấy cửa sổ nào của nó). */
        @Volatile var screenRequested = false

        // ---- Chỉ dùng cho nút Loa ----
        /** Lúc thấy màn hình cuộc gọi của ứng dụng (0 = chưa thấy). */
        @Volatile var windowSeenAt = 0L
        /** Vị trí nút Loa ở lần quét trước: chỉ chạm khi nút đã đứng yên (màn hình hết chuyển cảnh). */
        @Volatile var lastBounds: android.graphics.Rect? = null
        /** Đã chạm nút Loa; đang chờ xem Zalo có hiện bảng chọn "Loa ngoài / Tai nghe / Bluetooth" không. */
        @Volatile var tappedBounds: android.graphics.Rect? = null
        @Volatile var popupUntil = 0L
    }

    @Volatile var pending: Task? = null
        private set

    fun arm(context: Context, pkg: String, keywords: List<String>, timeoutMs: Long, onDone: (Boolean) -> Unit) {
        val service = BlindAccessibilityService.instance
        if (service == null) {
            onDone(false)
            return
        }
        val task = Task(pkg, keywords, System.currentTimeMillis() + timeoutMs, onDone)
        pending?.let { finish(it, false) }
        pending = task
        // Ngoài việc chờ sự kiện màn hình, tự quét lại định kỳ cho chắc.
        App.appScope.launch {
            while (!task.finished && System.currentTimeMillis() < task.deadline) {
                BlindAccessibilityService.instance?.tryRun(task)
                delay(700)
            }
            if (!task.finished) finish(task, false)
        }
    }

    fun finish(task: Task, ok: Boolean) {
        if (task.finished) return
        task.finished = true
        if (!ok) {
            // Không tìm thấy nút: lưu lại cấu trúc màn hình Zalo để người hỗ trợ gửi cho người sửa app.
            runCatching {
                BlindAccessibilityService.instance?.dumpWindows(task.pkg, task.keywords)?.let { Prefs.lastUiDump = it }
            }
        }
        if (pending === task) pending = null
        task.onDone(ok)
    }
}
