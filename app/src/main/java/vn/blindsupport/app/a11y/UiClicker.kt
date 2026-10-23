package vn.blindsupport.app.a11y

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.blindsupport.app.App

/**
 * "Hẹn" dịch vụ trợ năng bấm một nút trong Zalo/WhatsApp trong một khoảng thời gian ngắn
 * (ví dụ nút Loa ngay sau khi nghe máy). Ngoài khoảng thời gian đó app không đụng vào Zalo.
 */
object UiClicker {
    val SPEAKER_KEYS = listOf("loa", "loa ngoài", "speaker", "bật loa", "speakerphone")
    val ANSWER_KEYS = listOf("trả lời", "nghe", "answer", "accept", "chấp nhận")
    val DECLINE_KEYS = listOf("từ chối", "decline", "reject", "kết thúc", "end call", "hang up")

    class Task(
        val pkg: String,
        val keywords: List<String>,
        val deadline: Long,
        val onDone: (Boolean) -> Unit,
    ) {
        @Volatile var finished = false
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
        if (pending === task) pending = null
        task.onDone(ok)
    }
}
