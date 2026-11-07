package vn.blindsupport.app.core

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Bật loa ngoài cho cuộc gọi Zalo/WhatsApp bằng cách chuyển đường âm thanh của hệ thống,
 * không cần tìm nút "Loa" trên màn hình Zalo. Dùng khi dịch vụ trợ năng không bấm được nút
 * (giao diện Zalo mỗi máy một khác). Hết cuộc gọi thì trả lại như cũ.
 */
object CallSpeaker {
    private const val TAG = "CallSpeaker"
    @Volatile private var forced = false

    /** Trả về true nếu hệ thống báo đã chuyển sang loa ngoài. */
    fun force(context: Context): Boolean {
        val am = context.getSystemService(AudioManager::class.java) ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= 31) {
                val dev = am.availableCommunicationDevices
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } ?: return false
                val ok = am.setCommunicationDevice(dev)
                if (ok) forced = true
                ok && am.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = true
                forced = true
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn
            }
        } catch (e: Exception) {
            Log.w(TAG, "Không chuyển được sang loa ngoài", e)
            false
        }
    }

    /** Âm thanh cuộc gọi đang ra loa ngoài chưa. */
    fun isSpeakerActive(context: Context): Boolean {
        val am = context.getSystemService(AudioManager::class.java) ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= 31) {
                am.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn
            }
        }.getOrDefault(false)
    }

    /** Gọi khi cuộc gọi kết thúc: bỏ chế độ loa ngoài app đã ép. */
    fun release(context: Context) {
        if (!forced) return
        forced = false
        val am = context.getSystemService(AudioManager::class.java) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 31) {
                am.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = false
            }
        }
    }
}
