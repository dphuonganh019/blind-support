package vn.blindsupport.app.notify

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.blindsupport.app.App
import vn.blindsupport.app.call.CallManager
import vn.blindsupport.app.core.AudioState
import vn.blindsupport.app.core.MessageStore
import vn.blindsupport.app.core.Prefs
import vn.blindsupport.app.core.Speaker
import vn.blindsupport.app.core.StoredMessage
import vn.blindsupport.app.core.TextUtils
import vn.blindsupport.app.ui.VoiceSession
import vn.blindsupport.app.voip.VoipCallController
import java.util.Calendar

/**
 * Đọc to tin nhắn mới lần lượt từng tin. Không đọc khi đang có cuộc gọi, đang nghe lệnh,
 * máy để im lặng, hoặc trong giờ yên tĩnh — khi đó tin vẫn được lưu để nghe lại sau.
 */
object MessageAnnouncer {
    private sealed class Item {
        data class Msg(val msg: StoredMessage) : Item()
        data class Text(val text: String, val ignoreQuiet: Boolean) : Item()
    }

    private val queue = Channel<Item>(Channel.UNLIMITED)
    private var started = false
    @Volatile private var heldDuringCall = 0

    @Volatile private var appContext: Context? = null

    /** Giữ máy thức một lúc ngắn từ khi tin đến tới khi bắt đầu đọc (tự nhả sau 15 giây). */
    private fun bridgeWake() {
        val ctx = appContext ?: return
        runCatching {
            (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TroLyNgheGoi:queue")
                .apply { setReferenceCounted(false); acquire(15_000L) }
        }
    }

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        appContext = app
        App.appScope.launch {
            for (item in queue) {
                if (!Prefs.appEnabled) continue
                when (item) {
                    is Item.Msg -> {
                        val who = "${item.msg.app} từ ${item.msg.sender}"
                        if (isBusy(app)) {
                            heldDuringCall++
                            NotifLog.add("[không đọc] tin $who: đang có cuộc gọi / đang nghe lệnh")
                            continue
                        }
                        if (isQuiet(app)) {
                            Prefs.quietPending = true
                            NotifLog.add("[không đọc] tin $who: đang trong giờ yên tĩnh")
                            continue
                        }
                        if (item.msg.read) continue
                        NotifLog.add("[bắt đầu đọc] tin $who")
                        awake(app) { Speaker.speak(format(item.msg)) }
                        MessageStore.markRead(listOf(item.msg))
                        NotifLog.add("[đọc xong] tin $who")
                    }
                    is Item.Text -> {
                        if (isBusy(app)) continue
                        if (!item.ignoreQuiet && isQuiet(app)) continue
                        awake(app) { Speaker.speak(item.text) }
                    }
                }
                delay(400)
            }
        }
    }

    /**
     * Giữ máy thức (chỉ bộ xử lý, không bật màn hình) trong lúc đọc, để máy tắt màn hình không cho
     * app "ngủ" giữa chừng. Tự nhả sau khi đọc xong, hoặc tối đa 2 phút.
     */
    private suspend fun awake(context: Context, block: suspend () -> Unit) {
        val wl = runCatching {
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TroLyNgheGoi:read")
                .apply { setReferenceCounted(false); acquire(120_000L) }
        }.getOrNull()
        try {
            block()
        } finally {
            runCatching { if (wl?.isHeld == true) wl.release() }
        }
    }

    fun enqueueMessage(msg: StoredMessage) {
        bridgeWake()
        queue.trySend(Item.Msg(msg))
    }

    /** Thông báo hệ thống (pin, cuộc gọi nhỡ Zalo...). */
    fun enqueueText(text: String, ignoreQuiet: Boolean = false) {
        bridgeWake()
        queue.trySend(Item.Text(text, ignoreQuiet))
    }

    /** Gọi khi một cuộc gọi kết thúc: nhắc nếu có tin đến trong lúc gọi. */
    fun onCallEnded(context: Context) {
        val held = heldDuringCall
        heldDuringCall = 0
        if (held <= 0) return
        val unread = MessageStore.unreadCount()
        if (unread > 0) {
            enqueueText("Trong lúc gọi, bạn có $unread tin nhắn mới. Giữ phím giảm âm lượng rồi nói: đọc tin nhắn.")
        }
    }

    /** Gọi định kỳ: hết giờ yên tĩnh thì nhắc số tin nhắn đến trong lúc yên tĩnh (một lần). */
    fun remindAfterQuiet(context: Context) {
        if (!Prefs.quietPending || isQuiet(context) || isBusy(context)) return
        Prefs.quietPending = false
        val unread = MessageStore.unreadCount()
        if (unread > 0) {
            enqueueText("Trong giờ yên tĩnh, bạn có $unread tin nhắn mới. Giữ phím giảm âm lượng rồi nói: đọc tin nhắn.")
        }
    }

    fun format(m: StoredMessage): String {
        val text = TextUtils.limit(TextUtils.cleanForSpeech(m.text))
        val sender = TextUtils.cleanForSpeech(m.sender)
        return if (Prefs.readContent && text.isNotBlank()) {
            "Tin ${m.app} từ $sender: $text"
        } else {
            "Có tin ${m.app} mới từ $sender."
        }
    }

    private fun isBusy(context: Context): Boolean =
        AudioState.inAnyCall(context) ||
            CallManager.ringingCall() != null ||
            VoipCallController.isRinging() ||
            VoiceSession.active

    /**
     * Chỉ giờ yên tĩnh (nếu bật) mới chặn đọc tin. Máy để im lặng/rung KHÔNG chặn: người khiếm
     * thị không nhìn thấy biểu tượng im lặng nên sẽ tưởng app hỏng.
     */
    private fun isQuiet(context: Context): Boolean =
        Prefs.isQuietNow(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
}
