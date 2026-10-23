package vn.blindsupport.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import vn.blindsupport.app.call.CallNotifications
import vn.blindsupport.app.core.MessageStore
import vn.blindsupport.app.core.Prefs
import vn.blindsupport.app.core.RingVolumeGuard
import vn.blindsupport.app.core.Speaker
import vn.blindsupport.app.notify.MessageAnnouncer

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Prefs.init(this)
        MessageStore.init(this)
        CallNotifications.createChannels(this)
        Speaker.init(this)
        // Nếu lần trước app bị tắt đột ngột khi đang hạ nhỏ chuông thì trả lại âm lượng chuông.
        RingVolumeGuard.restoreIfStale(this)
        // Model nhận dạng giọng nói (~100MB RAM) KHÔNG nạp ở đây: chỉ nạp khi có cuộc gọi hoặc
        // khi mở lệnh giọng nói, để tiết kiệm pin và bộ nhớ khi app chỉ đọc tin nhắn.
        // Hàng đợi đọc tin/thông báo luôn sẵn sàng trong tiến trình.
        MessageAnnouncer.start(this)
        // Lên lịch "người gác" kiểm tra định kỳ.
        Watchdog.schedule(this)
    }

    companion object {
        lateinit var instance: App
            private set

        /** Phạm vi coroutine sống suốt vòng đời tiến trình. */
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
