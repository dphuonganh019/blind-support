package vn.blindsupport.app.notify

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.blindsupport.app.App
import vn.blindsupport.app.core.ContactsRepo
import vn.blindsupport.app.core.TextUtils
import vn.blindsupport.app.core.VoiceRecognizer
import vn.blindsupport.app.core.MessageStore
import vn.blindsupport.app.core.Prefs
import vn.blindsupport.app.core.RingVolumeGuard
import vn.blindsupport.app.voip.VoipCallController

/**
 * Dịch vụ đọc thông báo, được hệ thống giữ chạy thường trực khi đã cấp quyền.
 * Việc của nó:
 *  - đọc to tin nhắn Zalo, WhatsApp, SMS;
 *  - phát hiện cuộc gọi Zalo/WhatsApp đến để hỏi "có muốn nghe không";
 *  - báo pin yếu / cắm sạc / rút sạc.
 */
class BlindNotificationListener : NotificationListenerService() {

    private val ongoingVoipKeys = HashSet<String>()
    private var batteryRegistered = false

    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = true
        MessageAnnouncer.start(this)
        RingVolumeGuard.restoreIfStale(this)
        registerBattery()
        // Bắt kịp trạng thái cuộc gọi Zalo/WhatsApp nếu dịch vụ vừa được khởi động lại.
        ongoingVoipKeys.clear()
        runCatching { activeNotifications }.getOrNull()?.forEach { sbn ->
            val incoming = NotificationParser.incomingVoipCall(sbn)
            if (incoming != null) {
                if (Prefs.voipCalls) VoipCallController.onIncoming(this, incoming)
            } else if (NotificationParser.isOngoingVoipCall(sbn)) {
                ongoingVoipKeys += sbn.key
            }
        }
        voipCallInProgress = ongoingVoipKeys.isNotEmpty()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        connected = false
        unregisterBattery()
        // Samsung đôi khi ngắt dịch vụ; xin hệ thống kết nối lại ngay.
        runCatching { requestRebind(ComponentName(this, BlindNotificationListener::class.java)) }
    }

    override fun onDestroy() {
        unregisterBattery()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        try {
            handlePosted(sbn)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xử lý thông báo", e)
        }
    }

    private fun handlePosted(sbn: StatusBarNotification) {
        // 1) Cuộc gọi Zalo/WhatsApp đang đổ chuông
        NotificationParser.incomingVoipCall(sbn)?.let { info ->
            if (Prefs.voipCalls) VoipCallController.onIncoming(this, info)
            return
        }
        // 2) Cuộc gọi Zalo/WhatsApp đang diễn ra
        if (NotificationParser.isOngoingVoipCall(sbn)) {
            ongoingVoipKeys += sbn.key
            voipCallInProgress = true
            VoipCallController.onCallInProgress(sbn.key, sbn.notification?.contentIntent)
            return
        }
        // 3) Cuộc gọi nhỡ Zalo/WhatsApp
        NotificationParser.missedVoipCall(sbn)?.let { missed ->
            if (AppIds.readEnabled(sbn.packageName) || Prefs.voipCalls) {
                announceMissed(sbn.packageName, missed)
            }
            return
        }
        // 4) Tin nhắn
        if (!AppIds.readEnabled(sbn.packageName)) return
        for ((msg, isGroup) in NotificationParser.newMessages(sbn)) {
            if (isGroup && !Prefs.readGroups) {
                // Tin nhóm: chỉ lưu lại để nghe khi muốn, không đọc to.
                msg.read = false
                MessageStore.add(msg)
                continue
            }
            MessageStore.add(msg)
            MessageAnnouncer.enqueueMessage(msg)
        }
    }

    /**
     * Đọc to cuộc gọi nhỡ Zalo/WhatsApp, luôn kèm TÊN (danh bạ máy, sim, Zalo/WhatsApp) hoặc SỐ.
     * Zalo còn báo cả cuộc gọi SIM nhỡ từ người đã kết bạn: khi đó thông báo hay không ghi tên,
     * nên lấy tên/số từ nhật ký cuộc gọi sim vừa nhỡ và nói là "cuộc gọi" (không phải "cuộc gọi Zalo").
     */
    private fun announceMissed(pkg: String, missed: NotificationParser.MissedVoip) {
        val app = applicationContext
        App.appScope.launch {
            // Chờ nhật ký cuộc gọi sim kịp ghi.
            delay(2_000)
            val recentSim = withContext(Dispatchers.IO) {
                ContactsRepo.recentMissedCalls(app, withinMs = 3 * 60_000L).firstOrNull()
            }
            val numberName = missed.number?.let { withContext(Dispatchers.IO) { ContactsRepo.lookupName(app, it) } }
            val sameAsSim = recentSim != null && (
                (missed.name == null && missed.number == null) ||
                    (missed.number != null && recentSim.number != null &&
                        TextUtils.dialableNumber(missed.number) == TextUtils.dialableNumber(recentSim.number))
                )
            val who = when {
                sameAsSim -> recentSim!!.name ?: recentSim.number?.let { "số " + TextUtils.speakableNumber(it) }
                numberName != null -> numberName
                missed.name != null -> missed.name
                missed.number != null -> "số " + TextUtils.speakableNumber(missed.number)
                recentSim != null -> recentSim.name ?: recentSim.number?.let { "số " + TextUtils.speakableNumber(it) }
                else -> null
            } ?: "một người không rõ tên"
            val kind = if (sameAsSim) "cuộc gọi" else "cuộc gọi ${AppIds.label(pkg)}"
            VoiceRecognizer.log("[Nhỡ ${AppIds.label(pkg)}] ${missed.raw} → $kind từ $who", who != "một người không rõ tên")
            MessageAnnouncer.enqueueText("Bạn vừa nhỡ $kind từ ${TextUtils.cleanForSpeech(who)}.")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        VoipCallController.onNotificationRemoved(this, sbn.key)
        if (ongoingVoipKeys.remove(sbn.key)) {
            voipCallInProgress = ongoingVoipKeys.isNotEmpty()
            VoipCallController.dismissUi()
            if (ongoingVoipKeys.isEmpty()) VoipCallController.onCallEnded(this)
            MessageAnnouncer.onCallEnded(this)
        }
    }

    // ---------------- Pin ----------------

    private var lastPlugged: Boolean? = null
    private var fullAnnounced = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!Prefs.announceBattery) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            if (level < 0) return
            val pct = level * 100 / scale
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            val prev = lastPlugged
            lastPlugged = plugged

            if (prev != null && prev != plugged) {
                if (plugged) {
                    fullAnnounced = false
                    MessageAnnouncer.enqueueText("Đã cắm sạc. Pin $pct phần trăm.", ignoreQuiet = false)
                } else {
                    MessageAnnouncer.enqueueText("Đã rút sạc. Pin $pct phần trăm.", ignoreQuiet = false)
                }
            }

            if (plugged) {
                Prefs.lastBatteryLevelAnnounced = 100
                if (status == BatteryManager.BATTERY_STATUS_FULL && !fullAnnounced && prev != null) {
                    fullAnnounced = true
                    MessageAnnouncer.enqueueText("Pin đã sạc đầy.")
                }
                return
            }

            val threshold = listOf(5, 10, 20).firstOrNull { pct <= it } ?: return
            if (threshold < Prefs.lastBatteryLevelAnnounced) {
                Prefs.lastBatteryLevelAnnounced = threshold
                MessageAnnouncer.enqueueText(
                    "Pin yếu, chỉ còn $pct phần trăm. Hãy cắm sạc.",
                    ignoreQuiet = threshold <= 10
                )
            }
        }
    }

    private fun registerBattery() {
        if (batteryRegistered) return
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        runCatching {
            ContextCompat.registerReceiver(this, batteryReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
            batteryRegistered = true
        }
    }

    private fun unregisterBattery() {
        if (!batteryRegistered) return
        runCatching { unregisterReceiver(batteryReceiver) }
        batteryRegistered = false
    }

    companion object {
        private const val TAG = "BlindNLS"
        @Volatile var connected = false
            private set
        /** Đang có cuộc gọi Zalo/WhatsApp (theo thông báo "đang gọi" của chúng). */
        @Volatile var voipCallInProgress = false
            private set
    }
}
