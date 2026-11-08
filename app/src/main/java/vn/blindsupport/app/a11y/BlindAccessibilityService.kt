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
import vn.blindsupport.app.notify.CallScreenText
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
        if (!Prefs.appEnabled) return false
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
        if (event != null) checkIncomingCallScreen(event)
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
        val roots = mutableListOf<AccessibilityNodeInfo>()
        runCatching { windows }.getOrNull()?.forEach { w -> w.root?.let { roots += it } }
        rootInActiveWindow?.let { roots += it }
        if (speakerTask) {
            runSpeakerTask(task, roots)
            return
        }
        for (root in roots) {
            if (root.packageName?.toString() != task.pkg) continue
            val buttonTask = android.os.Build.VERSION.SDK_INT < 31 &&
                (task.keywords === UiClicker.ANSWER_KEYS || task.keywords === UiClicker.DECLINE_KEYS)
            when (findAndClick(root, task.keywords, allowTap = speakerTask, iconAbove = buttonTask)) {
                Result.CLICKED, Result.ALREADY_ON -> {
                    UiClicker.finish(task, true)
                    return
                }
                Result.NOT_FOUND -> Unit
            }
        }
    }

    private enum class Result { CLICKED, ALREADY_ON, NOT_FOUND }

    // ---------------- Android 11: thấy màn hình cuộc gọi Zalo đến ----------------

    enum class ScreenState { RINGING, OTHER, NOT_VISIBLE }

    private var lastIncomingCheck = 0L

    /**
     * Android 11 trở xuống: Zalo không dùng kiểu thông báo cuộc gọi chuẩn, nên app có thể không
     * biết có cuộc gọi. Nếu thấy màn hình "Cuộc gọi thoại đến" của Zalo thì báo cho bộ điều khiển.
     */
    private fun checkIncomingCallScreen(event: AccessibilityEvent) {
        if (android.os.Build.VERSION.SDK_INT >= 31) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != vn.blindsupport.app.notify.AppIds.ZALO) return
        // Chỉ xét lúc Zalo mở màn hình mới (không quét mỗi lần cuộn tin nhắn, cho máy khỏi chậm).
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (VoipCallController.isRinging()) return
        val now = SystemClock.uptimeMillis()
        if (now - lastIncomingCheck < 800) return
        lastIncomingCheck = now
        if (reportIncoming(pkg)) return
        // Màn hình vừa mở có thể chưa vẽ xong chữ: xem lại sau một chút.
        handler.postDelayed({ if (!VoipCallController.isRinging()) reportIncoming(pkg) }, 800)
    }

    private fun reportIncoming(pkg: String): Boolean {
        val caller = runCatching { incomingCaller(pkg) }.getOrNull() ?: return false
        VoipCallController.onIncomingScreen(this, pkg, caller)
        return true
    }

    /** Tên người gọi nếu màn hình cuộc gọi đến của [pkg] đang hiện ("" nếu không đọc được tên). */
    private fun incomingCaller(pkg: String): String? {
        for (root in appRoots(pkg)) {
            CallScreenText.incomingCaller(screenTexts(root))?.let { return it }
        }
        return null
    }

    /** Màn hình của [pkg] có đang hiện cuộc gọi đến không. */
    fun incomingScreenState(pkg: String): ScreenState {
        val roots = runCatching { appRoots(pkg) }.getOrDefault(emptyList())
        if (roots.isEmpty()) return ScreenState.NOT_VISIBLE
        return if (roots.any { CallScreenText.incomingCaller(screenTexts(it)) != null }) ScreenState.RINGING
        else ScreenState.OTHER
    }

    private fun appRoots(pkg: String): List<AccessibilityNodeInfo> {
        val roots = mutableListOf<AccessibilityNodeInfo>()
        runCatching { windows }.getOrNull()?.forEach { w -> w.root?.let { roots += it } }
        rootInActiveWindow?.let { roots += it }
        return roots.filter { it.packageName?.toString() == pkg }
    }

    /** Chữ trên màn hình theo thứ tự từ trên xuống (chữ và mô tả của từng nút). */
    private fun screenTexts(root: AccessibilityNodeInfo): List<String> {
        val out = ArrayList<String>()
        var visited = 0
        fun walk(n: AccessibilityNodeInfo, depth: Int) {
            if (visited++ > 400 || depth > 30) return
            n.text?.toString()?.takeIf { it.isNotBlank() }?.let { out += it }
            n.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out += it }
            for (i in 0 until n.childCount) n.getChild(i)?.let { walk(it, depth + 1) }
        }
        walk(root, 0)
        return out
    }

    /**
     * Bật loa ngoài trong cuộc gọi Zalo/WhatsApp, từng bước cho chắc:
     *  1. Màn hình cuộc gọi của ứng dụng phải đang hiện (không thì mở nó lên) và đã hiện ít nhất 1,5 giây.
     *  2. Cuộc gọi đã kết nối (có thông báo "đang trong cuộc gọi" hoặc đồng hồ đếm giờ trên màn hình).
     *  3. Nút Loa đứng yên ở cùng một chỗ qua 2 lần quét (màn hình hết chuyển cảnh) rồi mới chạm, MỘT lần.
     *  4. Trong 3 giây sau đó, nếu ứng dụng hiện bảng chọn (khi máy có Bluetooth) thì chạm "Loa ngoài".
     */
    private fun runSpeakerTask(task: UiClicker.Task, roots: List<AccessibilityNodeInfo>) {
        val now = System.currentTimeMillis()
        val appRoots = roots.filter { it.packageName?.toString() == task.pkg }

        // Bước 4: chờ bảng chọn sau khi đã chạm nút Loa.
        val tapped = task.tappedBounds
        if (tapped != null) {
            if (now > task.popupUntil) {
                UiClicker.finish(task, true)
                return
            }
            for (root in appRoots) {
                val option = findPopupSpeakerOption(root, tapped) ?: continue
                val target = clickableAncestor(option) ?: clickableSibling(option)
                val ok = (target != null && target.isEnabled && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) ||
                    tapCenter(option)
                if (ok) {
                    UiClicker.finish(task, true)
                    return
                }
            }
            return
        }

        // Android 11 trở xuống hỏi được âm thanh đã ra loa ngoài chưa: rồi thì không chạm (chạm sẽ TẮT loa).
        if (android.os.Build.VERSION.SDK_INT < 31 && CallSpeaker.isSpeakerActive(this)) {
            UiClicker.finish(task, true)
            return
        }
        // Bước 1.
        if (appRoots.isEmpty()) {
            task.windowSeenAt = 0L
            task.lastBounds = null
            if (!task.screenRequested) {
                task.screenRequested = true
                VoipCallController.bringCallScreen(this)
            }
            return
        }
        if (task.windowSeenAt == 0L) task.windowSeenAt = now
        if (now - task.windowSeenAt < 1_500L) return
        // Bước 2.
        val connected = vn.blindsupport.app.notify.BlindNotificationListener.voipCallInProgress ||
            appRoots.any { hasCallTimer(it) }
        if (!connected) return
        // Bước 3.
        val normKeys = UiClicker.SPEAKER_KEYS.map { " " + TextUtils.normalize(it) + " " }
        val node = appRoots.firstNotNullOfOrNull { findFirst(it) { n -> matches(n, normKeys) } } ?: return
        if ((node.isCheckable && node.isChecked) || node.isSelected) {
            UiClicker.finish(task, true)
            return
        }
        val l = label(node)
        if (l.contains(" tắt loa ") || l.contains(" speaker on ")) {
            UiClicker.finish(task, true)
            return
        }
        val r = android.graphics.Rect()
        node.getBoundsInScreen(r)
        if (r.isEmpty || task.lastBounds != r) {
            task.lastBounds = r
            return
        }
        val target = clickableAncestor(node) ?: clickableSibling(node)
        val ok = (target != null && target.isEnabled && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) ||
            tapCenter(node)
        if (!ok) return
        task.tappedBounds = r
        task.popupUntil = now + 3_000L
        task.deadline = maxOf(task.deadline, task.popupUntil + 1_000L)
    }

    /** Có đồng hồ đếm giờ cuộc gọi ("00:05") trên màn hình: cuộc gọi đã kết nối. */
    private fun hasCallTimer(root: AccessibilityNodeInfo): Boolean = findFirst(root) { n ->
        n.className?.toString()?.contains("Chronometer") == true ||
            Regex("^\\d{1,2}:\\d{2}(:\\d{2})?$").matches(n.text?.toString()?.trim().orEmpty())
    } != null

    /** Mục "Loa ngoài" trong bảng chọn âm thanh, nằm ngoài vùng nút Loa vừa chạm. */
    private fun findPopupSpeakerOption(root: AccessibilityNodeInfo, tapped: android.graphics.Rect): AccessibilityNodeInfo? {
        val keys = listOf(" loa ngoài ", " speaker ", " speakerphone ", " loa ")
        return findFirst(root) { n ->
            val l = label(n)
            if (keys.none { l.contains(it) }) return@findFirst false
            if (l.contains(" tai nghe ") || l.contains(" bluetooth ") || l.contains(" headset ")) return@findFirst false
            if (l.contains("speakerbtn")) return@findFirst false
            val b = android.graphics.Rect()
            n.getBoundsInScreen(b)
            if (b.isEmpty || android.graphics.Rect.intersects(b, tapped)) return@findFirst false
            // Không chọn lại chính nút Loa trên màn hình cuộc gọi.
            var p: AccessibilityNodeInfo? = n.parent
            var depth = 0
            while (p != null && depth < 6) {
                if (p.viewIdResourceName?.lowercase()?.contains("speakerbtn") == true) return@findFirst false
                p = p.parent
                depth++
            }
            true
        }
    }

    /** Duyệt cây giao diện theo chiều rộng, trả về nút đầu tiên thỏa [test]. */
    private fun findFirst(root: AccessibilityNodeInfo, test: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 800) {
            val node = queue.removeFirst()
            visited++
            if (runCatching { test(node) }.getOrDefault(false)) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }

    /**
     * [allowTap]: nút không nhận lệnh "bấm" của trợ năng (Zalo tự xử lý cú chạm) thì chạm giả lập
     * bằng ngón tay ảo vào giữa nút. Chỉ dùng cho nút Loa.
     */
    private fun findAndClick(
        root: AccessibilityNodeInfo,
        keywords: List<String>,
        allowTap: Boolean = false,
        iconAbove: Boolean = false,
    ): Result {
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
                // Màn hình cuộc gọi Zalo (Android 11): chữ "Trả lời" / "Từ chối" nằm DƯỚI nút tròn.
                if (target == null && iconAbove && clickIconAbove(root, node)) return Result.CLICKED
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

    /**
     * Bấm nút tròn nằm ngay phía trên chữ [label] (kiểu nút Trả lời / Từ chối của Zalo).
     * Tìm nút gần nhất thẳng hàng phía trên; không thấy thì chạm vào chỗ ngay trên chữ.
     */
    private fun clickIconAbove(root: AccessibilityNodeInfo, label: AccessibilityNodeInfo): Boolean {
        val lb = android.graphics.Rect()
        label.getBoundsInScreen(lb)
        if (lb.isEmpty) return false
        val screenW = resources.displayMetrics.widthPixels
        var best: AccessibilityNodeInfo? = null
        var bestScore = Int.MAX_VALUE
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 800) {
            val n = queue.removeFirst()
            visited++
            for (i in 0 until n.childCount) n.getChild(i)?.let { queue.add(it) }
            val b = android.graphics.Rect()
            n.getBoundsInScreen(b)
            if (b.isEmpty || b == lb || b.width() < 20 || b.width() > screenW * 6 / 10) continue
            if (b.contains(lb)) continue
            val gap = lb.top - b.bottom
            if (gap < -lb.height() || gap > lb.height() * 4) continue
            if (kotlin.math.abs(b.centerX() - lb.centerX()) > maxOf(lb.width(), b.width()) / 2) continue
            // Ưu tiên nút bấm được, rồi tới nút gần chữ nhất.
            val score = (if (isClickableNode(n)) 0 else 10_000) + kotlin.math.abs(gap)
            if (score < bestScore) {
                bestScore = score
                best = n
            }
        }
        val icon = best
        if (icon != null) {
            if (isClickableNode(icon) && icon.isEnabled && icon.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            if (tapCenter(icon)) return true
        }
        // Không thấy nút: chạm ngay phía trên chữ.
        val x = lb.exactCenterX()
        val y = (lb.top - lb.height() * 1.5f).coerceAtLeast(1f)
        val path = android.graphics.Path().apply { moveTo(x, y) }
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
