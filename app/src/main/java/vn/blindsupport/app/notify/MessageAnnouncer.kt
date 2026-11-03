package vn.blindsupport.app.notify

import android.content.Context
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

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        App.appScope.launch {
            for (item in queue) {
                when (item) {
                    is Item.Msg -> {
                        if (isBusy(app)) {
                            heldDuringCall++
                            continue
                        }
                        if (isQuiet(app)) continue
                        if (item.msg.read) continue
                        Speaker.speak(format(item.msg))
                        MessageStore.markRead(listOf(item.msg))
                    }
                    is Item.Text -> {
                        if (isBusy(app)) continue
                        if (!item.ignoreQuiet && isQuiet(app)) continue
                        Speaker.speak(item.text)
                    }
                }
                delay(400)
            }
        }
    }

    fun enqueueMessage(msg: StoredMessage) {
        queue.trySend(Item.Msg(msg))
    }

    /** Thông báo hệ thống (pin, cuộc gọi nhỡ Zalo...). */
    fun enqueueText(text: String, ignoreQuiet: Boolean = false) {
        queue.trySend(Item.Text(text, ignoreQuiet))
    }

    /** Gọi khi một cuộc gọi kết thúc: nhắc nếu có tin đến trong lúc gọi. */
    fun onCallEnded(context: Context) {
        val held = heldDuringCall
        heldDuringCall = 0
        if (held <= 0) return
        val unread = MessageStore.unreadCount()
        if (unread > 0) {
            enqueueText("Trong lúc gọi, bạn có $unread tin nhắn mới. Bấm đúp nút nguồn rồi nói: đọc tin nhắn.")
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
