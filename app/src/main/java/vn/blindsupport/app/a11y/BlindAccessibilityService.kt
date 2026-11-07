package vn.blindsupport.app.a11y

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import vn.blindsupport.app.call.CallManager
import vn.blindsupport.app.core.AudioState
import vn.blindsupport.app.core.Prefs
import vn.blindsupport.app.core.TextUtils
import vn.blindsupport.app.ui.VoiceCommandActivity
import vn.blindsupport.app.ui.VoiceSession
import vn.blindsupport.app.voip.VoipCallController

/**
 * Dịch vụ trợ năng: chỉ hoạt động trong Zalo/WhatsApp và chỉ khi app vừa "hẹn" bấm một nút
 * (nút Loa sau khi nghe máy, hoặc nút Trả lời/Từ chối nếu thông báo không có sẵn nút).
 */
class BlindAccessibilityService : AccessibilityService() {

    private var lastScan = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    // ---------------- Giữ phím giảm âm lượng = mở lệnh giọng nói ----------------

    private val handler = Handler(Looper.getMainLooper())
    /** Đang theo dõi một lần nhấn giữ phím giảm âm lượng. */
    private var tracking = false
    private var triggered = false
    private val holdRunnable = Runnable {
        if (tracking) {
            triggered = true
            openVoiceCommand()
        }
    }

    /**
     * Hệ thống gửi MỌI phím bấm qua đây trước. Trả về true = app giữ lại phím đó.
     * Chỉ can thiệp phím giảm âm lượng, và chỉ khi không có cuộc gọi nào.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        // Phím nhả mà app không giữ lúc nhấn: để hệ thống xử lý như thường.
        if (event.action == KeyEvent.ACTION_UP && !tracking) return false
        if (!tracking && !shouldIntercept()) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0 && !tracking) {
                    tracking = true
                    triggered = false
                    handler.postDelayed(holdRunnable, Prefs.volumeHoldMs.toLong())
                }
                return true
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(holdRunnable)
                val wasTriggered = triggered
                tracking = false
                triggered = false
                if (!wasTriggered) {
                    // Bấm nhanh khi app đang đọc trong màn nghe lệnh (ví dụ đọc tin nhắn): dừng đọc.
                    val stop = VoiceSession.onStopRequested
                    if (VoiceSession.active && stop != null) {
                        runCatching { stop() }
                        return true
                    }
                    // Bấm nhanh: giảm âm lượng như bình thường (vì app đã giữ lại phím này).
                    runCatching {
                        getSystemService(AudioManager::class.java)?.adjustSuggestedStreamVolume(
                            AudioManager.ADJUST_LOWER, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI
                        )
                    }
                }
                return true
            }
        }
        return true
    }

    private fun shouldIntercept(): Boolean {
        if (!Prefs.volumeKeyTrigger) return false
        // Đang có cuộc gọi: để phím âm lượng cho màn hình cuộc gọi (tăng = nghe, giảm = tắt chuông)
        // và cho việc chỉnh âm lượng cuộc gọi.
        if (CallManager.calls().isNotEmpty() || VoipCallController.isRinging() || AudioState.inAnyCall(this)) {
            return false
        }
        // Màn hình tắt vẫn nhận: người khiếm thị không biết màn đang tắt hay sáng. Giữ đủ lâu
        // (mặc định 1,5 giây) mới mở lệnh nên không sợ chạm nhầm trong túi.
        return true
    }

    private fun openVoiceCommand() {
        vibrate()
        wakeScreen()
        val i = Intent(this, VoiceCommandActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        runCatching { startActivity(i) }
    }

    /** Bật sáng màn hình nếu đang tắt, để màn nghe lệnh hiện được lên trên màn khóa. */
    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        val pm = getSystemService(PowerManager::class.java) ?: return
        if (pm.isInteractive) return
        runCatching {
            pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "TroLyNgheGoi:wake"
            ).acquire(3_000L)
        }
    }

    private fun vibrate() {
        runCatching {
            getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(250, 255))
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val task = UiClicker.pending ?: return
        if (event?.packageName?.toString() != task.pkg) return
        val now = SystemClock.uptimeMillis()
        if (now - lastScan < 250) return
        lastScan = now
        tryRun(task)
    }

    /** Tìm và bấm nút phù hợp. Gọi được từ luồng chính. */
    fun tryRun(task: UiClicker.Task) {
        if (task.finished) return
        if (System.currentTimeMillis() > task.deadline) {
            UiClicker.finish(task, false)
            return
        }
        val roots = mutableListOf<AccessibilityNodeInfo>()
        runCatching { windows }.getOrNull()?.forEach { w -> w.root?.let { roots += it } }
        rootInActiveWindow?.let { roots += it }
        for (root in roots) {
            if (root.packageName?.toString() != task.pkg) continue
            when (findAndClick(root, task.keywords)) {
                Result.CLICKED, Result.ALREADY_ON -> {
                    UiClicker.finish(task, true)
                    return
                }
                Result.NOT_FOUND -> Unit
            }
        }
    }

    private enum class Result { CLICKED, ALREADY_ON, NOT_FOUND }

    private fun findAndClick(root: AccessibilityNodeInfo, keywords: List<String>): Result {
        val normKeys = keywords.map { " " + TextUtils.normalize(it) + " " }
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 800) {
            val node = queue.removeFirst()
            visited++
            if (matches(node, normKeys)) {
                // Nút loa dạng bật/tắt đã ở trạng thái bật: không bấm nữa kẻo lại tắt.
                if ((node.isCheckable && node.isChecked) || node.isSelected) return Result.ALREADY_ON
                val label = label(node)
                if (label.contains(" tắt loa ") || label.contains(" speaker on ")) return Result.ALREADY_ON
                val target = clickableAncestor(node)
                if (target != null && target.isEnabled &&
                    target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ) return Result.CLICKED
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return Result.NOT_FOUND
    }

    private fun label(node: AccessibilityNodeInfo): String {
        val parts = listOfNotNull(
            node.text?.toString(),
            node.contentDescription?.toString(),
            node.viewIdResourceName?.substringAfter(":id/")?.replace('_', ' '),
        )
        return " " + TextUtils.normalize(parts.joinToString(" ")) + " "
    }

    private fun matches(node: AccessibilityNodeInfo, normKeys: List<String>): Boolean {
        val l = label(node)
        if (l.isBlank()) return false
        return normKeys.any { l.contains(it) }
    }

    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var cur: AccessibilityNodeInfo? = node
        var depth = 0
        while (cur != null && depth < 4) {
            if (cur.isClickable) return cur
            cur = cur.parent
            depth++
        }
        return null
    }

    companion object {
        @Volatile var instance: BlindAccessibilityService? = null
            private set
    }
}
