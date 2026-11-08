package vn.blindsupport.app.voip

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import vn.blindsupport.app.App
import vn.blindsupport.app.a11y.UiClicker
import vn.blindsupport.app.core.AppVolume
import vn.blindsupport.app.core.AudioState
import vn.blindsupport.app.core.CallSpeaker
import vn.blindsupport.app.core.Prefs
import vn.blindsupport.app.core.Speaker
import vn.blindsupport.app.core.TextUtils
import vn.blindsupport.app.core.VoiceCallPrompt
import vn.blindsupport.app.core.VoiceRecognizer
import vn.blindsupport.app.notify.AppIds
import vn.blindsupport.app.notify.VoipCallInfo

/**
 * Điều khiển cuộc gọi Zalo/WhatsApp đến, dựa trên thông báo cuộc gọi của chính Zalo/WhatsApp:
 *  - đọc tên người gọi và hỏi "có muốn nghe không";
 *  - "nghe": bấm nút Trả lời có sẵn trong thông báo, rồi nhờ dịch vụ trợ năng bấm nút Loa;
 *  - "không": bấm nút Từ chối trong thông báo.
 */
object VoipCallController {
    private const val TAG = "VoipCall"

    enum class State { IDLE, RINGING, ANSWERED }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    @Volatile var current: VoipCallInfo? = null
        private set
    /** Bấm vào thông báo "đang trong cuộc gọi" của Zalo: mở lại màn hình cuộc gọi của Zalo. */
    @Volatile private var ongoingIntent: PendingIntent? = null
    private var promptJob: Job? = null

    fun isRinging(): Boolean = _state.value == State.RINGING

    fun appName(): String = current?.let { AppIds.label(it.pkg) } ?: ""

    /** Vừa gọi đi qua Zalo/WhatsApp: thông báo "đang gọi" của cuộc gọi đi không phải cuộc gọi đến. */
    @Volatile private var outgoingSince = 0L
    @Volatile private var outgoingPkg: String? = null

    fun isRecentOutgoing(pkg: String): Boolean =
        pkg == outgoingPkg && System.currentTimeMillis() - outgoingSince < 90_000L

    /** App vừa mở cuộc gọi đi qua [pkg]: bật loa ngoài như khi nghe máy. */
    fun onOutgoingStarted(context: Context, pkg: String) {
        outgoingPkg = pkg
        outgoingSince = System.currentTimeMillis()
        enableSpeaker(context.applicationContext, pkg, outgoing = true)
    }

    /** Vừa nghe máy Zalo/WhatsApp: Zalo hay đăng lại thông báo "cuộc gọi đến" một lần nữa ngay sau đó. */
    @Volatile private var answeredPkg: String? = null
    @Volatile private var answeredAt = 0L
    /** Cuộc gọi vừa nghe được nhận ra theo cách Android 11 (legacy): khóa thông báo của nó. */
    @Volatile private var answeredLegacyKey: String? = null

    /**
     * Thông báo [key] là của cuộc gọi Zalo vừa nghe (nhận theo cách Android 11): coi là "đang trong
     * cuộc gọi" để biết lúc cuộc gọi kết thúc. Cuộc gọi nhận theo kiểu chuẩn không bị ảnh hưởng.
     */
    fun isLegacyAnsweredKey(key: String): Boolean = key == answeredLegacyKey

    fun onIncoming(context: Context, info: VoipCallInfo) {
        if (!Prefs.appEnabled) return
        if (isRecentOutgoing(info.pkg)) return
        // Thông báo "cuộc gọi đến" của chính cuộc gọi vừa nghe: bỏ qua, kẻo bật lại màn hình trợ lý
        // và hỏi lại (làm màn hình nhảy qua lại, chạm hụt nút Loa).
        if (info.pkg == answeredPkg && System.currentTimeMillis() - answeredAt < 30_000L) return
        val existing = current
        if (existing != null && existing.key == info.key && _state.value == State.RINGING) {
            // Thông báo được cập nhật: giữ nút mới nhất.
            current = info
            return
        }
        current = info
        _state.value = State.RINGING
        Speaker.stop()
        // Nạp model nhận dạng giọng nói song song với lúc đọc tên người gọi.
        VoiceRecognizer.preload(context.applicationContext, App.appScope)
        VoipCallActivity.start(context)

        val app = context.applicationContext
        val spokenName = TextUtils.cleanForSpeech(info.callerName).ifBlank { "người gọi không rõ tên" }
        promptJob?.cancel()
        promptJob = App.appScope.launch {
            // Zalo/WhatsApp cũng mở màn hình riêng cùng lúc: chờ một chút rồi đưa màn hình của app
            // lên trên cùng lần nữa (app phải đang hiện trên màn hình mới được dùng micro).
            delay(700)
            if (isRinging()) VoipCallActivity.start(app)
            delay(300)
            VoiceCallPrompt(
                context = app,
                callerLabel = spokenName,
                appName = AppIds.label(info.pkg),
                isRinging = { isRinging() && current?.key == info.key },
                onAnswer = { answer(app) },
                onDecline = { decline(app) },
            ).run()
        }
    }

    /** Thông báo kiểu "cuộc gọi" gần nhất (Android 11: Zalo dùng cùng một thông báo lúc reo và lúc gọi). */
    @Volatile private var lastCallNotifKey: String? = null
    @Volatile private var lastCallNotifAt = 0L
    /** Thông báo cuộc gọi của cuộc gọi đang hỏi (nhận theo cách Android 11): gỡ đi = cuộc gọi đã dừng reo. */
    @Volatile private var linkedRingKey: String? = null

    /** Thông báo "đang trong cuộc gọi" của Zalo/WhatsApp xuất hiện. */
    fun onCallInProgress(key: String, contentIntent: PendingIntent?) {
        if (contentIntent != null) ongoingIntent = contentIntent
        lastCallNotifKey = key
        lastCallNotifAt = System.currentTimeMillis()
        if (_state.value == State.RINGING && current?.legacy == true) {
            // Android 11: Zalo đưa lên thông báo cuộc gọi (không chữ, không nút) ngay khi đang REO,
            // giống hệt lúc đã nghe máy. Không được coi là đã nghe máy, kẻo app thôi hỏi và đóng
            // màn hình của app sau 1 giây. Chỉ nhớ thông báo này để biết lúc cuộc gọi dừng reo.
            linkedRingKey = key
            return
        }
        if (_state.value == State.RINGING) {
            stopPrompt()
            _state.value = State.ANSWERED
        }
    }

    fun onNotificationRemoved(context: Context, key: String) {
        val c0 = current
        if (c0 != null && c0.legacy && key == linkedRingKey && _state.value == State.RINGING) {
            endRinging(c0.key, "Zalo gỡ thông báo cuộc gọi (người gọi đã cúp hoặc đã nghe/từ chối trên Zalo)")
            return
        }
        val c = current ?: return
        if (c.key != key) return
        if (_state.value == State.RINGING) {
            // Người gọi cúp máy hoặc đã nghe/từ chối trên màn hình Zalo.
            stopPrompt()
            _state.value = State.IDLE
            current = null
        }
    }

    fun answer(context: Context) {
        val info = current ?: return
        answeredPkg = info.pkg
        answeredAt = System.currentTimeMillis()
        answeredLegacyKey = if (info.legacy) info.key else null
        stopPrompt()
        _state.value = State.ANSWERED
        val sent = info.answer?.let { send(context, it) } ?: false
        if (info.legacy) vn.blindsupport.app.notify.NotifLog.add("[nghe] ${AppIds.label(info.pkg)}: ${if (sent) "bấm nút Trả lời trong thông báo" else "nhờ trợ năng bấm Trả lời"}")
        if (!sent) {
            // Không có nút trong thông báo: nhờ trợ năng bấm nút "Trả lời" trên màn hình Zalo.
            UiClicker.arm(context, info.pkg, UiClicker.ANSWER_KEYS, 8_000) { ok ->
                if (info.legacy) vn.blindsupport.app.notify.NotifLog.add("[nghe] trợ năng bấm Trả lời: ${if (ok) "được" else "KHÔNG được"}")
                if (ok) afterAnswered(context, info) else App.appScope.launch {
                    Speaker.speak("Không tự nghe máy được. Hãy vuốt hoặc bấm nút nghe trên màn hình.")
                }
            }
            // Màn hình của app vừa đóng; nếu màn hình cuộc gọi Zalo không hiện ra thì mở nó lên.
            if (info.legacy) {
                App.appScope.launch {
                    delay(1_500)
                    val task = UiClicker.pending
                    if (task != null && task.pkg == info.pkg && !task.finished) {
                        info.open?.let { send(context, it) }
                    }
                }
            }
            return
        }
        afterAnswered(context, info)
    }

    private fun afterAnswered(context: Context, info: VoipCallInfo) {
        enableSpeaker(context, info.pkg, outgoing = false)
    }

    private fun enableSpeaker(context: Context, pkg: String, outgoing: Boolean) {
        App.appScope.launch {
            for (wait in longArrayOf(800, 1500, 3000, 5000)) {
                delay(wait)
                AppVolume.maxCallVolume(context)
            }
        }
        if (!Prefs.autoSpeaker || AudioState.hasHeadset(context)) return
        // Cách 1: chuyển âm thanh cuộc gọi sang loa ngoài ở mức hệ thống (không cần giao diện Zalo).
        // Lặp lại vài lần vì Zalo có thể tự trả về loa trong lúc đang kết nối cuộc gọi.
        var routed = false
        val routeJob = App.appScope.launch {
            // Android 12 trở lên: hệ thống ưu tiên lựa chọn của Zalo, lệnh chuyển loa từ app khác không
            // có tác dụng mà còn làm app tưởng loa đã bật. Chỉ dùng cách bấm nút Loa.
            if (Build.VERSION.SDK_INT >= 31) return@launch
            for (wait in longArrayOf(1_200, 1_500, 2_000, 3_000, 4_000)) {
                delay(wait)
                val active = _state.value == State.ANSWERED ||
                    vn.blindsupport.app.notify.BlindNotificationListener.voipCallInProgress ||
                    (outgoing && isRecentOutgoing(pkg))
                if (!active) break
                if (CallSpeaker.force(context)) routed = true
            }
        }
        // Cách 2: nhờ trợ năng bấm nút "Loa" trên màn hình cuộc gọi của Zalo.
        // Gọi đi phải chờ bên kia nghe máy nên cho lâu hơn.
        UiClicker.arm(context, pkg, UiClicker.SPEAKER_KEYS, if (outgoing) 45_000 else 15_000) { ok ->
            if (ok && Build.VERSION.SDK_INT < 31) {
                // Android 11 trở xuống kiểm tra lại được: chạm rồi mà loa vẫn chưa bật thì báo.
                App.appScope.launch {
                    delay(1_500)
                    if (!CallSpeaker.isSpeakerActive(context)) {
                        Speaker.speak("Chưa bật được loa ngoài, hãy áp máy lên tai.")
                    }
                }
            }
            if (!ok) {
                App.appScope.launch {
                    routeJob.join()
                    if (!routed) {
                        Speaker.speak(
                            if (outgoing) "Chưa bật được loa ngoài, hãy áp máy lên tai."
                            else "Đã nghe máy. Chưa bật được loa ngoài, hãy áp máy lên tai."
                        )
                    }
                }
            }
        }
        // Màn hình cuộc gọi Zalo có thể chưa hiện (máy khóa, nghe từ thông báo): mở nó lên để
        // trợ năng tìm được nút Loa.
        App.appScope.launch {
            delay(2_500)
            val task = UiClicker.pending
            if (task != null && task.pkg == pkg && !task.finished) {
                ongoingIntent?.let { send(context, it) }
            }
        }
    }

    /** Mở lại màn hình cuộc gọi của Zalo/WhatsApp (bấm vào thông báo "đang trong cuộc gọi"). */
    fun bringCallScreen(context: Context) {
        ongoingIntent?.let { send(context, it) }
    }

    /** Cuộc gọi Zalo/WhatsApp đã kết thúc. */
    fun onCallEnded(context: Context) {
        ongoingIntent = null
        answeredLegacyKey = null
        // Cuộc gọi đã xong: cuộc gọi đến tiếp theo (kể cả ngay sau đó) phải được báo bình thường.
        answeredPkg = null
        answeredAt = 0L
        CallSpeaker.release(context)
    }

    fun decline(context: Context) {
        val info = current ?: return
        stopPrompt()
        _state.value = State.IDLE
        current = null
        val sent = info.decline?.let { send(context, it) } ?: false
        if (!sent) {
            UiClicker.arm(context, info.pkg, UiClicker.DECLINE_KEYS, 6_000) { }
        }
    }

    // ---------- Android 11: nhận cuộc gọi Zalo từ màn hình cuộc gọi của Zalo ----------

    @Volatile private var screenCheckAt = 0L

    /**
     * Dịch vụ trợ năng thấy màn hình "Cuộc gọi thoại đến" của Zalo (có nút Trả lời / Từ chối).
     * Chỉ dùng khi thông báo không cho biết có cuộc gọi (Android 11 trở xuống): chờ một chút, nếu
     * cách nhận qua thông báo vẫn chưa thấy cuộc gọi thì hỏi người dùng như bình thường.
     */
    fun onIncomingScreen(context: Context, pkg: String, callerName: String) {
        if (Build.VERSION.SDK_INT >= 31) return
        if (!Prefs.appEnabled || !Prefs.voipCalls) return
        if (isRinging()) return
        if (isRecentOutgoing(pkg)) return
        val now = System.currentTimeMillis()
        if (pkg == answeredPkg && now - answeredAt < 30_000L) return
        if (now - screenCheckAt < 3_000L) return
        screenCheckAt = now
        val app = context.applicationContext
        App.appScope.launch {
            delay(1_200)
            if (isRinging()) return@launch
            val svc = vn.blindsupport.app.a11y.BlindAccessibilityService.instance ?: return@launch
            if (svc.incomingScreenState(pkg) != vn.blindsupport.app.a11y.BlindAccessibilityService.ScreenState.RINGING) return@launch
            val key = "screen:$pkg:${System.currentTimeMillis()}"
            // Thông báo cuộc gọi Zalo vừa đăng (nếu có) là của cuộc gọi này.
            linkedRingKey = lastCallNotifKey?.takeIf { System.currentTimeMillis() - lastCallNotifAt < 15_000L }
            vn.blindsupport.app.notify.NotifLog.add("[màn hình] thấy cuộc gọi ${AppIds.label(pkg)} đến từ \"$callerName\" → hỏi nghe máy")
            onIncoming(app, VoipCallInfo(key, pkg, callerName.ifBlank { "người gọi không rõ tên" }, null, null, legacy = true))
            watchScreenCall(key, pkg)
        }
    }

    /**
     * Cuộc gọi nhận từ màn hình không có thông báo để biết lúc người gọi cúp máy: theo dõi màn hình
     * Zalo (nếu đang hiện), cuộc gọi nhỡ, và dừng hẳn sau 60 giây.
     */
    private fun watchScreenCall(key: String, pkg: String) {
        App.appScope.launch {
            val start = System.currentTimeMillis()
            var otherCount = 0
            while (isRinging() && current?.key == key) {
                delay(1_000)
                if (System.currentTimeMillis() - start > 60_000L) {
                    endRinging(key, "hết 60 giây")
                    break
                }
                val st = vn.blindsupport.app.a11y.BlindAccessibilityService.instance?.incomingScreenState(pkg)
                if (st == vn.blindsupport.app.a11y.BlindAccessibilityService.ScreenState.OTHER) {
                    if (++otherCount >= 2) {
                        endRinging(key, "màn hình Zalo không còn cuộc gọi")
                        break
                    }
                } else {
                    otherCount = 0
                }
            }
        }
    }

    /** Zalo/WhatsApp báo cuộc gọi nhỡ: cuộc gọi đang hỏi (nhận theo cách Android 11) đã kết thúc. */
    fun onMissed(pkg: String) {
        val c = current ?: return
        if (c.legacy && c.pkg == pkg && isRinging()) endRinging(c.key, "có thông báo cuộc gọi nhỡ")
    }

    private fun endRinging(key: String, reason: String) {
        val c = current ?: return
        if (c.key != key || _state.value != State.RINGING) return
        vn.blindsupport.app.notify.NotifLog.add("[màn hình] dừng hỏi: $reason")
        linkedRingKey = null
        stopPrompt()
        _state.value = State.IDLE
        current = null
    }

    /** Ngừng đọc/hỏi nhưng vẫn để cuộc gọi đổ chuông. */
    fun stopAsking() {
        stopPrompt()
    }

    fun dismissUi() {
        if (_state.value == State.ANSWERED) {
            _state.value = State.IDLE
            current = null
        }
    }

    private fun stopPrompt() {
        promptJob?.cancel()
        promptJob = null
        Speaker.stop()
    }

    private fun send(context: Context, pi: PendingIntent): Boolean = try {
        if (Build.VERSION.SDK_INT >= 34) {
            val opts = ActivityOptions.makeBasic()
            @Suppress("DEPRECATION")
            opts.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            pi.send(context, 0, null, null, null, null, opts.toBundle())
        } else {
            pi.send()
        }
        true
    } catch (e: Exception) {
        Log.w(TAG, "Không bấm được nút trong thông báo", e)
        false
    }
}
