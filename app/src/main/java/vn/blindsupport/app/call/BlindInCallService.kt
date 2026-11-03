package vn.blindsupport.app.call

import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.blindsupport.app.core.AppVolume
import vn.blindsupport.app.core.AudioState
import vn.blindsupport.app.core.Prefs
import vn.blindsupport.app.core.RingVolumeGuard
import vn.blindsupport.app.core.Speaker
import vn.blindsupport.app.core.VoiceCallPrompt
import vn.blindsupport.app.core.VoiceRecognizer
import vn.blindsupport.app.notify.MessageAnnouncer

/**
 * Hệ thống Android gọi vào đây cho MỌI cuộc gọi sim khi app là ứng dụng điện thoại mặc định.
 * Không cần chạy ngầm liên tục: Android tự khởi động app khi có cuộc gọi.
 */
class BlindInCallService : InCallService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val promptJobs = HashMap<Call, Job>()
    private val answeredCalls = HashSet<Call>()
    private val spokenNames = HashMap<Call, String>()

    private var endpoints: List<CallEndpoint> = emptyList()
    private var currentEndpoint: CallEndpoint? = null

    override fun onCreate() {
        super.onCreate()
        CallManager.service = this
        VoiceRecognizer.preload(this, scope)
    }

    override fun onDestroy() {
        CallManager.service = null
        scope.cancel()
        RingVolumeGuard.restore(this)
        CallNotifications.cancel(this)
        CallManager.notifyChanged()
        super.onDestroy()
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            onCallStateChanged(call, state)
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            CallManager.notifyChanged()
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callback)
        // Có cuộc gọi: dừng ngay việc đang đọc tin nhắn.
        Speaker.stop()
        val spoken = call.spokenName(this)
        spokenNames[call] = spoken
        CallManager.notifyChanged()

        val state = call.stateCompat()
        if (state == Call.STATE_RINGING) {
            CallNotifications.showIncoming(this, call.displayName(this))
            InCallActivity.start(this)
            val others = calls.count { it != call && it.stateCompat() != Call.STATE_DISCONNECTED }
            if (others > 0) {
                // Đang nói chuyện mà có cuộc gọi chờ: micro đang bận nên chỉ báo, không hỏi.
                scope.launch { Speaker.speak("Có cuộc gọi chờ từ $spoken.") }
            } else {
                startPrompt(call, spoken)
            }
        } else {
            // Cuộc gọi đi.
            CallNotifications.showOngoing(this, call.displayName(this))
            InCallActivity.start(this)
            routeToSpeakerWithRetry()
        }
    }

    private fun startPrompt(call: Call, spoken: String) {
        promptJobs[call]?.cancel()
        promptJobs[call] = scope.launch {
            VoiceCallPrompt(
                context = this@BlindInCallService,
                callerLabel = spoken,
                appName = null,
                isRinging = { call.stateCompat() == Call.STATE_RINGING },
                onAnswer = { CallManager.answer(call) },
                onDecline = { CallManager.decline(call) },
            ).run()
        }
    }

    private fun stopPrompt(call: Call) {
        promptJobs.remove(call)?.let {
            it.cancel()
            Speaker.stop()
        }
        RingVolumeGuard.restore(this)
    }

    private fun onCallStateChanged(call: Call, state: Int) {
        CallManager.notifyChanged()
        if (state != Call.STATE_RINGING) stopPrompt(call)
        when (state) {
            Call.STATE_ACTIVE -> {
                answeredCalls += call
                CallNotifications.showOngoing(this, call.displayName(this))
                routeToSpeakerWithRetry()
            }
            Call.STATE_DIALING, Call.STATE_CONNECTING -> routeToSpeakerWithRetry()
            else -> Unit
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callback)
        stopPrompt(call)
        val spoken = spokenNames.remove(call) ?: "số không xác định"
        val wasAnswered = answeredCalls.remove(call)
        val cause = call.details.disconnectCause?.code
        CallManager.notifyChanged()
        if (cause == DisconnectCause.MISSED) CallNotifications.showMissed(this, call.displayName(this))

        val remaining = calls.filter { it.stateCompat() != Call.STATE_DISCONNECTED }
        if (remaining.isEmpty()) CallNotifications.cancel(this)

        scope.launch {
            delay(700)
            when {
                cause == DisconnectCause.MISSED -> {
                    Prefs.lastMissedNumber = call.number()
                    Prefs.lastMissedName = spoken
                    Speaker.speak(
                        "Bạn vừa nhỡ cuộc gọi từ $spoken. Muốn gọi lại, hãy bấm đúp nút nguồn rồi nói: gọi lại."
                    )
                }
                wasAnswered -> Speaker.speak("Đã kết thúc cuộc gọi.")
                cause == DisconnectCause.REJECTED -> Unit
                else -> Speaker.speak("Cuộc gọi đã kết thúc.")
            }
            if (remaining.isEmpty()) MessageAnnouncer.onCallEnded(this@BlindInCallService)
        }
    }

    // ---------------- Loa ngoài ----------------

    private fun routeToSpeakerWithRetry() {
        scope.launch {
            for (wait in longArrayOf(0, 600, 1500, 3000)) {
                delay(wait)
                // Âm lượng cuộc gọi tách riêng cho loa ngoài và loa trong, nên đặt lại sau mỗi lần đổi.
                AppVolume.maxCallVolume(this@BlindInCallService)
                if (!Prefs.autoSpeaker || AudioState.hasHeadset(this@BlindInCallService)) continue
                if (!isSpeakerOn()) setSpeaker(true)
            }
        }
    }

    fun isSpeakerOn(): Boolean {
        if (Build.VERSION.SDK_INT >= 34) {
            currentEndpoint?.let { return it.endpointType == CallEndpoint.TYPE_SPEAKER }
        }
        @Suppress("DEPRECATION")
        return callAudioState?.route == CallAudioState.ROUTE_SPEAKER
    }

    fun setSpeaker(on: Boolean) {
        if (Build.VERSION.SDK_INT >= 34 && endpoints.isNotEmpty()) {
            val wanted = if (on) {
                endpoints.firstOrNull { it.endpointType == CallEndpoint.TYPE_SPEAKER }
            } else {
                endpoints.firstOrNull { it.endpointType == CallEndpoint.TYPE_WIRED_HEADSET }
                    ?: endpoints.firstOrNull { it.endpointType == CallEndpoint.TYPE_BLUETOOTH }
                    ?: endpoints.firstOrNull { it.endpointType == CallEndpoint.TYPE_EARPIECE }
            }
            if (wanted != null) {
                requestCallEndpointChange(wanted, mainExecutor,
                    object : OutcomeReceiver<Void, CallEndpointException> {
                        override fun onResult(result: Void?) {}
                        override fun onError(error: CallEndpointException) {
                            Log.w("InCall", "Đổi loa thất bại", error)
                            setSpeakerLegacy(on)
                        }
                    })
                return
            }
        }
        setSpeakerLegacy(on)
    }

    @Suppress("DEPRECATION")
    private fun setSpeakerLegacy(on: Boolean) {
        runCatching {
            setAudioRoute(if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        @Suppress("DEPRECATION")
        super.onCallAudioStateChanged(audioState)
        CallManager.notifyChanged()
    }

    override fun onAvailableCallEndpointsChanged(availableEndpoints: MutableList<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(availableEndpoints)
        endpoints = availableEndpoints.toList()
    }

    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        super.onCallEndpointChanged(callEndpoint)
        currentEndpoint = callEndpoint
        CallManager.notifyChanged()
    }
}
