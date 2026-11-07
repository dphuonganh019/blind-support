package vn.blindsupport.app.core

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.delay

/** Tiếng "bíp" báo hiệu "bắt đầu nói đi": to, trầm và dài để người già kịp nhận ra. */
object Beeper {
    suspend fun beep() {
        if (!Speaker.longBeep(Prefs.beepMs)) {
            // Dự phòng nếu phát không được: tiếng bíp hệ thống.
            val tg = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME) }.getOrNull() ?: return
            try {
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, Prefs.beepMs)
                delay(Prefs.beepMs + 100L)
            } finally {
                tg.release()
            }
        }
        // Chờ tiếng vang tắt hẳn rồi mới bật micro.
        delay(200)
    }
}

/**
 * Tạm hạ nhỏ chuông khi app đang nói/nghe lệnh để người dùng nghe rõ và micro không bị
 * tiếng chuông lấn át. Luôn lưu lại mức cũ vào bộ nhớ để nếu app bị tắt đột ngột thì
 * lần chạy sau vẫn trả lại được.
 */
object RingVolumeGuard {
    private const val TAG = "RingVolumeGuard"

    fun lower(context: Context) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val cur = am.getStreamVolume(AudioManager.STREAM_RING)
        if (cur <= 1) return
        if (Prefs.savedRingVolume < 0) {
            Prefs.savedRingVolume = cur
            Prefs.savedRingTime = System.currentTimeMillis()
        }
        try {
            am.setStreamVolume(AudioManager.STREAM_RING, 1, 0)
        } catch (e: SecurityException) {
            Log.w(TAG, "Không hạ được chuông", e)
        }
    }

    fun restore(context: Context) {
        val v = Prefs.savedRingVolume
        if (v < 0) return
        val am = context.getSystemService(AudioManager::class.java) ?: return
        try {
            if (am.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                am.setStreamVolume(AudioManager.STREAM_RING, v, 0)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Không trả lại được chuông", e)
        }
        Prefs.savedRingVolume = -1
    }

    /**
     * Có cuộc gọi đến: kéo âm lượng chuông máy lên tối đa (nhớ mức cũ để trả lại khi xong).
     * Máy đang ở chế độ im lặng/rung thì Android không cho đổi – app vẫn đọc tên bằng "Chuông báo".
     */
    fun raiseForCall(context: Context) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val cur = am.getStreamVolume(AudioManager.STREAM_RING)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_RING)
        if (cur >= max) return
        if (Prefs.savedCallRingVolume < 0) {
            Prefs.savedCallRingVolume = cur
            Prefs.savedCallRingTime = System.currentTimeMillis()
        }
        try {
            am.setStreamVolume(AudioManager.STREAM_RING, max, 0)
        } catch (e: SecurityException) {
            Log.w(TAG, "Không kéo được chuông lên", e)
        }
    }

    /** Cuộc gọi đã được nghe / từ chối / kết thúc: trả chuông máy về mức người dùng đã đặt. */
    fun endCall(context: Context) {
        val v = Prefs.savedCallRingVolume
        if (v < 0) return
        val am = context.getSystemService(AudioManager::class.java)
        try {
            if (am != null && am.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                am.setStreamVolume(AudioManager.STREAM_RING, v, 0)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Không trả lại được chuông", e)
        }
        Prefs.savedCallRingVolume = -1
    }

    /** Gọi khi app khởi động: nếu còn mức chuông cũ chưa trả lại (app bị tắt giữa chừng) thì trả lại. */
    fun restoreIfStale(context: Context) {
        if (Prefs.savedCallRingVolume >= 0 &&
            System.currentTimeMillis() - Prefs.savedCallRingTime > 5 * 60_000L
        ) {
            restore(context)
            endCall(context)
        }
        if (Prefs.savedRingVolume >= 0 &&
            System.currentTimeMillis() - Prefs.savedRingTime > 2 * 60_000L
        ) {
            restore(context)
        }
    }
}

/** Âm lượng giọng đọc của app (luồng báo thức). */
object AppVolume {
    private fun am(context: Context) = context.getSystemService(AudioManager::class.java)

    fun percent(context: Context): Int {
        val am = am(context) ?: return 0
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        return am.getStreamVolume(AudioManager.STREAM_ALARM) * 100 / max
    }

    /** Đảm bảo giọng đọc không quá nhỏ (dùng trước khi báo cuộc gọi). */
    fun ensureAtLeast(context: Context, percent: Int) {
        val am = am(context) ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val min = (max * percent + 99) / 100
        if (am.getStreamVolume(AudioManager.STREAM_ALARM) < min) {
            runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, min, 0) }
        }
    }

    fun setPercent(context: Context, percent: Int) {
        val am = am(context) ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val v = (max * percent.coerceIn(10, 100) + 50) / 100
        runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, v.coerceAtLeast(1), 0) }
    }

    /** Đặt âm lượng "Chuông báo" theo cài đặt của app (gọi trước mỗi lần đọc). */
    fun applyVoiceVolume(context: Context) {
        setPercent(context, Prefs.voiceVolume)
    }

    /**
     * Tăng độ to một nấc: trước hết tăng âm lượng máy tới 100%, sau đó tăng mức khuếch đại.
     * Trả về câu để đọc cho người dùng.
     */
    fun louder(context: Context): String {
        return when {
            Prefs.voiceVolume < 100 -> {
                Prefs.voiceVolume = Prefs.voiceVolume + 20
                applyVoiceVolume(context)
                "Đã tăng âm lượng, ${Prefs.voiceVolume} phần trăm."
            }
            Prefs.boostLevel < 4 -> {
                Prefs.boostLevel = Prefs.boostLevel + 1
                "Đã tăng độ to lên mức ${Prefs.boostLevel}."
            }
            else -> "Đã to nhất rồi."
        }
    }

    /** Giảm độ to một nấc: giảm mức khuếch đại trước, rồi mới giảm âm lượng máy. */
    fun softer(context: Context): String {
        return when {
            Prefs.boostLevel > 1 -> {
                Prefs.boostLevel = Prefs.boostLevel - 1
                "Đã giảm độ to xuống mức ${Prefs.boostLevel}."
            }
            Prefs.voiceVolume > 30 -> {
                Prefs.voiceVolume = Prefs.voiceVolume - 20
                applyVoiceVolume(context)
                "Đã giảm âm lượng, ${Prefs.voiceVolume} phần trăm."
            }
            else -> "Đã nhỏ nhất rồi."
        }
    }

    /** Đưa âm lượng cuộc gọi lên tối đa (giọng người bên kia, cả loa ngoài lẫn loa trong). */
    fun maxCallVolume(context: Context) {
        if (!Prefs.maxCallVolume) return
        val am = context.getSystemService(AudioManager::class.java) ?: return
        runCatching {
            am.setStreamVolume(
                AudioManager.STREAM_VOICE_CALL, am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0
            )
        }
    }
}

object AudioState {
    /** Có tai nghe có dây hoặc Bluetooth đang kết nối không. */
    fun hasHeadset(context: Context): Boolean {
        val am = context.getSystemService(AudioManager::class.java) ?: return false
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        }
    }

    /** Đang trong một cuộc gọi bất kỳ (sim, Zalo, WhatsApp...). */
    fun inAnyCall(context: Context): Boolean {
        // Không dựa vào MODE_IN_COMMUNICATION: Zalo/WhatsApp có thể để chế độ này cả khi chỉ phát
        // tin nhắn thoại hay video, làm app tưởng đang gọi và không đọc tin nhắn nữa.
        val telecom = runCatching {
            context.getSystemService(android.telecom.TelecomManager::class.java)?.isInCall == true
        }.getOrDefault(false)
        val am = context.getSystemService(AudioManager::class.java)
        return telecom || am?.mode == AudioManager.MODE_IN_CALL ||
            vn.blindsupport.app.notify.BlindNotificationListener.voipCallInProgress
    }

    fun isRingerNormal(context: Context): Boolean {
        val am = context.getSystemService(AudioManager::class.java) ?: return true
        return am.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }
}
