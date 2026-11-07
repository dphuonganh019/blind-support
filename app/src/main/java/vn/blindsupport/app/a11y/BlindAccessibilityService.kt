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
import vn.blindsupport.app.core.CallSpeaker
import vn.blindsupport.app.core.Speaker
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
    /** Đã nghe máy bằng phím trong lần nhấn này: giữ lại cả lúc nhả phím. */
    private var answeredByKey = false

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val volumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        // Đang có cuộc gọi đến đổ chuông: phím âm lượng bất kỳ = nghe máy.
        if (volumeKey) {
            val voipRinging = VoipCallController.isRinging()
            val simRinging = CallManager.ringingCall()
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && (voipRinging || simRinging != null)) {
                answeredByKey = true
                Speaker.stop()
                if (voipRinging) VoipCallController.answer(this)
                else if (simRinging != null && CallManager.calls().size == 1) CallManager.answer(simRinging)
                return true
            }
            if (answeredByKey) {
                if (event.action == KeyEvent.ACTION_UP) answeredByKey = false
                return true
            }
        }
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
        val speakerTask = task.keywords === UiClicker.SPEAKER_KEYS
        // Âm thanh đã ra loa ngoài rồi: không chạm vào nút Loa nữa (chạm sẽ TẮT loa).
        if (speakerTask && CallSpeaker.isSpeakerActive(this)) {
            UiClicker.finish(task, true)
            return
        }
        val roots = mutableListOf<AccessibilityNodeInfo>()
        runCatching { windows }.getOrNull()?.forEach { w -> w.root?.let { roots += it } }
        rootInActiveWindow?.let { roots += it }
        for (root in roots) {
            if (root.packageName?.toString() != task.pkg) continue
            when (findAndClick(root, task.keywords, allowTap = speakerTask)) {
                Result.CLICKED, Result.ALREADY_ON -> {
                    UiClicker.finish(task, true)
                    return
                }
                Result.NOT_FOUND -> Unit
            }
        }
    }

    private enum class Result { CLICKED, ALREADY_ON, NOT_FOUND }

    /**
     * [allowTap]: nút không nhận lệnh "bấm" của trợ năng (Zalo tự xử lý cú chạm) thì chạm giả lập
     * bằng ngón tay ảo vào giữa nút. Chỉ dùng cho nút Loa.
     */
    private fun findAndClick(root: AccessibilityNodeInfo, keywords: List<String>, allowTap: Boolean = false): Result {
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
                // Chữ "Loa" thường nằm dưới một nút hình (biểu tượng) không có chữ: nếu bản thân
                // nó và các tầng cha đều không bấm được thì bấm nút anh em cạnh nó.
                val target = clickableAncestor(node) ?: clickableSibling(node)
                if (target != null && target.isEnabled &&
                    target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ) return Result.CLICKED
                if (target == null && allowTap && tapCenter(node)) return Result.CLICKED
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

    /** Chạm giả lập vào giữa nút trên màn hình. */
    private fun tapCenter(node: AccessibilityNodeInfo): Boolean {
        val r = android.graphics.Rect()
        node.getBoundsInScreen(r)
        if (r.isEmpty || r.width() < 4 || r.height() < 4) return false
        val path = android.graphics.Path().apply { moveTo(r.exactCenterX(), r.exactCenterY()) }
        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        return runCatching { dispatchGesture(gesture, null, null) }.getOrDefault(false)
    }

    private fun isClickableNode(n: AccessibilityNodeInfo): Boolean =
        n.isClickable || n.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }

    /** Nút bấm được nằm trong cùng nhóm (cùng cha) với [node]. */
    private fun clickableSibling(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val parent = node.parent ?: return null
        val candidates = (0 until parent.childCount).mapNotNull { parent.getChild(it) }
            .filter { it != node && isClickableNode(it) && it.isEnabled }
        return candidates.singleOrNull()
    }

    /**
     * Ghi lại các nút trên màn hình của [pkg] (chữ, mô tả, mã nút, có bấm được không) để người
     * sửa app biết nút "Loa" của Zalo trên máy này trông thế nào.
     */
    fun dumpWindows(pkg: String, keywords: List<String>): String {
        val sb = StringBuilder()
        sb.append("Tìm: ").append(keywords.joinToString(", ")).append('\n')
        sb.append("Máy: ").append(android.os.Build.MANUFACTURER).append(' ').append(android.os.Build.MODEL)
            .append(", Android ").append(android.os.Build.VERSION.RELEASE).append('\n')
        runCatching { packageManager.getPackageInfo(pkg, 0).versionName }.getOrNull()?.let {
            sb.append(pkg).append(' ').append(it).append('\n')
        }
        val roots = mutableListOf<AccessibilityNodeInfo>()
        runCatching { windows }.getOrNull()?.forEach { w -> w.root?.let { roots += it } }
        rootInActiveWindow?.let { roots += it }
        sb.append("Cửa sổ: ").append(roots.joinToString { it.packageName?.toString() ?: "?" }).append('\n')
        for (root in roots) {
            if (root.packageName?.toString() != pkg) continue
            fun walk(n: AccessibilityNodeInfo, depth: Int) {
                if (sb.length > 50_000 || depth > 30) return
                val text = n.text?.toString().orEmpty()
                val desc = n.contentDescription?.toString().orEmpty()
                val id = n.viewIdResourceName?.substringAfter(":id/").orEmpty()
                val cls = n.className?.toString()?.substringAfterLast('.').orEmpty()
                sb.append("  ".repeat(depth.coerceAtMost(12))).append(cls)
                if (id.isNotEmpty()) sb.append(" #").append(id)
                if (text.isNotEmpty()) sb.append(" \"").append(text.take(40)).append('"')
                if (desc.isNotEmpty()) sb.append(" [").append(desc.take(40)).append(']')
                if (isClickableNode(n)) sb.append(" *bấm")
                if (n.isCheckable) sb.append(if (n.isChecked) " (bật)" else " (tắt)")
                sb.append('\n')
                for (i in 0 until n.childCount) n.getChild(i)?.let { walk(it, depth + 1) }
            }
            walk(root, 0)
        }
        return sb.toString()
    }

    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var cur: AccessibilityNodeInfo? = node
        var depth = 0
        while (cur != null && depth < 6) {
            if (isClickableNode(cur)) return cur
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
