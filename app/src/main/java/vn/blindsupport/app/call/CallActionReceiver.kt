package vn.blindsupport.app.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import vn.blindsupport.app.core.Speaker

/** Nút "Nghe" / "Từ chối" / "Kết thúc" trên thông báo cuộc gọi. */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Speaker.stop()
        when (intent.action) {
            ANSWER -> CallManager.ringingCall()?.let { CallManager.answer(it) }
            DECLINE -> CallManager.ringingCall()?.let { CallManager.decline(it) }
            HANGUP -> CallManager.primaryCall()?.let { CallManager.hangup(it) }
        }
    }

    companion object {
        const val ANSWER = "vn.blindsupport.app.ANSWER"
        const val DECLINE = "vn.blindsupport.app.DECLINE"
        const val HANGUP = "vn.blindsupport.app.HANGUP"
    }
}
