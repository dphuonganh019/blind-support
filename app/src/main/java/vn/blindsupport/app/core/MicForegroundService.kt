package vn.blindsupport.app.core

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import vn.blindsupport.app.R
import vn.blindsupport.app.call.CallNotifications

/**
 * Dịch vụ tiền cảnh loại "micro", chỉ chạy trong lúc app hỏi "nghe hay không" khi có cuộc gọi.
 *
 * Khi app đang chạy nền, một số máy (ColorOS) không cho dùng micro nếu màn hình cuộc gọi không
 * bật lên được. Dịch vụ này giữ quyền dùng micro trong vài chục giây hỏi trả lời.
 *
 * Chỉ dùng trên Android 13 trở xuống: Android 14+ cấm khởi động dịch vụ micro từ nền và sẽ
 * làm app bị lỗi; trên các máy đó app vẫn dựa vào màn hình cuộc gọi như trước.
 * Không khởi động được thì [start] trả false và app chạy như cũ.
 */
class MicForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n = Notification.Builder(this, CallNotifications.CH_STATUS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Đang nghe câu trả lời")
            .setOngoing(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(ID, n)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Không bật được dịch vụ micro", e)
        }
        running = true
        // Đã được yêu cầu dừng trước khi kịp chạy: dừng ngay sau khi đã gọi startForeground
        // (dừng trước bước này sẽ làm app bị hệ thống báo lỗi).
        if (stopRequested) {
            stopRequested = false
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MicFgService"
        private const val ID = 1010
        @Volatile private var running = false
        @Volatile private var stopRequested = false

        fun start(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= 34) return false
            return try {
                CallNotifications.createChannels(context)
                stopRequested = false
                context.startForegroundService(Intent(context, MicForegroundService::class.java))
                true
            } catch (e: Exception) {
                Log.w(TAG, "Không khởi động được dịch vụ micro", e)
                false
            }
        }

        fun stop(context: Context) {
            if (!running) {
                stopRequested = true
                return
            }
            runCatching { context.stopService(Intent(context, MicForegroundService::class.java)) }
        }
    }
}
