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
    private var promptJob: Job? = null

    fun isRinging(): Boolean = _state.value == State.RINGING

    fun appName(): String = current?.let { AppIds.label(it.pkg) } ?: ""

    fun onIncoming(context: Context, info: VoipCallInfo) {
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

    /** Thông báo "đang trong cuộc gọi" của Zalo/WhatsApp xuất hiện. */
    fun onCallInProgress(key: String) {
        if (_state.value == State.RINGING) {
            stopPrompt()
            _state.value = State.ANSWERED
        }
    }

    fun onNotificationRemoved(context: Context, key: String) {
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
        stopPrompt()
        _state.value = State.ANSWERED
        val sent = info.answer?.let { send(context, it) } ?: false
        if (!sent) {
            // Không có nút trong thông báo: nhờ trợ năng bấm nút "Trả lời" trên màn hình Zalo.
            UiClicker.arm(context, info.pkg, UiClicker.ANSWER_KEYS, 8_000) { ok ->
                if (ok) afterAnswered(context, info) else App.appScope.launch {
                    Speaker.speak("Không tự nghe máy được. Hãy vuốt hoặc bấm nút nghe trên màn hình.")
                }
            }
            return
        }
        afterAnswered(context, info)
    }

    private fun afterAnswered(context: Context, info: VoipCallInfo) {
        App.appScope.launch {
            for (wait in longArrayOf(800, 1500, 3000, 5000)) {
                delay(wait)
                AppVolume.maxCallVolume(context)
            }
        }
        if (!Prefs.autoSpeaker || AudioState.hasHeadset(context)) return
        UiClicker.arm(context, info.pkg, UiClicker.SPEAKER_KEYS, 12_000) { ok ->
            if (!ok) {
                App.appScope.launch {
                    Speaker.speak("Đã nghe máy. Chưa bật được loa ngoài, hãy áp máy lên tai.")
                }
            }
        }
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
