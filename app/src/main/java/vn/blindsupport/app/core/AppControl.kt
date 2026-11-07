package vn.blindsupport.app.core

import android.content.Context
import vn.blindsupport.app.Watchdog
import vn.blindsupport.app.a11y.BlindAccessibilityService
import vn.blindsupport.app.voip.VoipCallController

/**
 * Công tắc tổng "Tạm tắt hết" / "Bật lại" trong Cài đặt Trợ Lý.
 *
 * Tạm tắt: app ngừng mọi việc (không đọc, không hỏi, không bấm gì), tự tắt Trợ năng của mình,
 * trả lại âm lượng và ngừng "người gác". Riêng ứng dụng điện thoại mặc định thì Android bắt buộc
 * người dùng tự chọn lại – Cài đặt Trợ Lý mở sẵn màn hình đó. Trong lúc chưa chọn lại, app vẫn
 * hiện màn hình cuộc gọi bình thường để máy không bao giờ mất cuộc gọi.
 */
object AppControl {
    fun disableAll(context: Context) {
        Prefs.appEnabled = false
        Speaker.stop()
        runCatching { VoipCallController.stopAsking() }
        RingVolumeGuard.restore(context)
        CallSpeaker.release(context)
        MicForegroundService.stop(context)
        Watchdog.cancel(context)
        // Trợ năng tự tắt: phím giảm âm lượng trở lại bình thường.
        runCatching { BlindAccessibilityService.instance?.disableSelf() }
    }

    fun enableAll(context: Context) {
        Prefs.appEnabled = true
        Watchdog.schedule(context)
    }
}
