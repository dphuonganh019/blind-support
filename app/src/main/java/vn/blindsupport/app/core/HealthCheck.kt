package vn.blindsupport.app.core

import android.Manifest
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import vn.blindsupport.app.a11y.BlindAccessibilityService
import vn.blindsupport.app.notify.BlindNotificationListener

/**
 * Kiểm tra app đã được cấp đủ quyền chưa. Dùng ở màn hình cài đặt (cho người hỗ trợ)
 * và khi người dùng mở lệnh giọng nói (đọc to cảnh báo nếu thiếu thứ quan trọng).
 */
object HealthCheck {

    data class Item(
        val key: String,
        val title: String,
        val ok: Boolean,
        /** Thiếu thì tính năng chính hỏng: sẽ đọc to cảnh báo cho người dùng. */
        val critical: Boolean,
        val spokenWarning: String,
    )

    fun run(context: Context): List<Item> {
        val list = mutableListOf<Item>()
        list += Item(
            "dialer", "Là ứng dụng điện thoại mặc định", isDefaultDialer(context), true,
            "chưa được đặt làm ứng dụng điện thoại mặc định, nên sẽ không báo được cuộc gọi đến"
        )
        list += perm(context, Manifest.permission.RECORD_AUDIO, "mic", "Quyền micro", true,
            "chưa có quyền micro, nên không nghe được lệnh giọng nói")
        list += perm(context, Manifest.permission.READ_CONTACTS, "contacts", "Quyền danh bạ", true,
            "chưa có quyền danh bạ, nên không đọc được tên người gọi")
        list += perm(context, Manifest.permission.CALL_PHONE, "call", "Quyền gọi điện", true,
            "chưa có quyền gọi điện")
        list += perm(context, Manifest.permission.READ_CALL_LOG, "calllog", "Quyền nhật ký cuộc gọi", false,
            "chưa có quyền nhật ký cuộc gọi")
        list += perm(context, Manifest.permission.READ_PHONE_STATE, "phone", "Quyền trạng thái điện thoại", false,
            "chưa có quyền trạng thái điện thoại")
        if (Build.VERSION.SDK_INT >= 33) {
            list += perm(context, Manifest.permission.POST_NOTIFICATIONS, "notif", "Quyền hiện thông báo", false,
                "chưa có quyền hiện thông báo")
        }
        list += Item(
            "listener", "Quyền đọc thông báo (tin Zalo, WhatsApp, SMS)", isNotificationListenerEnabled(context), true,
            "chưa có quyền đọc thông báo, nên không đọc được tin nhắn Zalo và không báo được cuộc gọi Zalo"
        )
        list += Item(
            "a11y", "Hỗ trợ tiếp cận (bấm loa Zalo/WhatsApp)", isAccessibilityEnabled(context), false,
            "chưa bật trợ năng, nên không tự bật được loa ngoài trong cuộc gọi Zalo"
        )
        list += Item(
            "overlay", "Hiển thị trên ứng dụng khác", Settings.canDrawOverlays(context), false,
            "chưa có quyền hiển thị trên ứng dụng khác"
        )
        list += Item(
            "battery", "Không giới hạn pin (chạy ngầm ổn định)", isIgnoringBattery(context), false,
            "chưa được miễn tối ưu pin"
        )
        list += Item(
            "autorevoke", "Không tự thu hồi quyền khi lâu không mở app", isAutoRevokeOff(context), false,
            "có thể bị hệ thống tự thu hồi quyền khi lâu không mở"
        )
        list += Item(
            "fullscreen", "Hiện màn hình cuộc gọi khi khóa máy", canFullScreen(context), false,
            "chưa có quyền hiện màn hình cuộc gọi khi khóa máy"
        )
        list += Item(
            "tts", "Giọng đọc tiếng Việt", Speaker.vietnameseAvailable, true,
            "chưa có giọng đọc tiếng Việt"
        )
        list += Item(
            "model", "Bộ nhận dạng giọng nói offline", VoiceRecognizer.isModelLoaded, true,
            "bộ nhận dạng giọng nói chưa sẵn sàng"
        )
        return list
    }

    private fun perm(
        c: Context, p: String, key: String, title: String, critical: Boolean, warn: String,
    ) = Item(key, title, c.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED, critical, warn)

    fun isDefaultDialer(context: Context): Boolean {
        val rm = context.getSystemService(RoleManager::class.java) ?: return false
        return rm.isRoleAvailable(RoleManager.ROLE_DIALER) && rm.isRoleHeld(RoleManager.ROLE_DIALER)
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        return nm.isNotificationListenerAccessGranted(
            ComponentName(context, BlindNotificationListener::class.java)
        )
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(context, BlindAccessibilityService::class.java)
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it)?.let { cn -> cn == me } ?: false
        }
    }

    fun isIgnoringBattery(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isAutoRevokeOff(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 30) return true
        // Ứng dụng điện thoại mặc định được hệ thống miễn thu hồi quyền (công tắc bị khóa xám).
        if (isDefaultDialer(context)) return true
        return runCatching { context.packageManager.isAutoRevokeWhitelisted }.getOrDefault(false)
    }

    fun canFullScreen(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        return nm.canUseFullScreenIntent()
    }

    /** Câu cảnh báo đọc to cho người dùng nếu thiếu thứ quan trọng, hoặc null nếu mọi thứ ổn. */
    fun spokenProblems(context: Context): String? {
        val bad = run(context).filter { it.critical && !it.ok && it.key != "model" }
        if (bad.isEmpty()) return null
        return "Cảnh báo: ứng dụng " + bad.joinToString("; ") { it.spokenWarning } +
            ". Hãy nhờ người hỗ trợ mở mục Cài đặt Trợ Lý."
    }
}
