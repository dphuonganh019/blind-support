package vn.blindsupport.app.a11y

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import vn.blindsupport.app.core.TextUtils

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
                if (node.isCheckable && node.isChecked) return Result.ALREADY_ON
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
