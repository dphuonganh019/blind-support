package vn.blindsupport.app.call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telecom.Call
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.blindsupport.app.core.Speaker
import vn.blindsupport.app.ui.BigUi

/**
 * Màn hình cuộc gọi sim.
 * - Đang đổ chuông: nửa trên NGHE (xanh), nửa dưới TỪ CHỐI (đỏ). Phím tăng âm lượng = nghe, giảm = từ chối.
 * - Đang gọi: nửa trên bật/tắt loa, nửa dưới GIỮ để kết thúc (giữ lâu để tránh chạm nhầm).
 */
class InCallActivity : ComponentActivity() {

    private lateinit var nameView: TextView
    private lateinit var statusView: TextView
    private lateinit var topButton: Button
    private lateinit var bottomButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BigUi.showOverLockScreen(this)

        val root = BigUi.column(this)
        nameView = BigUi.title(this, 34f)
        statusView = BigUi.title(this, 22f)
        topButton = BigUi.bigButton(this, BigUi.GREEN)
        bottomButton = BigUi.bigButton(this, BigUi.RED)
        root.addView(nameView)
        root.addView(statusView)
        root.addView(topButton)
        root.addView(bottomButton)
        setContentView(root)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { CallManager.version.collect { render() } }
                launch {
                    while (true) {
                        delay(1000)
                        render()
                    }
                }
            }
        }
    }

    private fun render() {
        val call = CallManager.primaryCall()
        if (call == null) {
            finish()
            return
        }
        nameView.text = call.displayName(this)
        when (call.stateCompat()) {
            Call.STATE_RINGING -> {
                statusView.text = "Cuộc gọi đến"
                topButton.text = "NGHE"
                BigUi.setButtonColor(topButton, BigUi.GREEN)
                topButton.setOnClickListener {
                    Speaker.stop()
                    CallManager.answer(call)
                }
                topButton.setOnLongClickListener(null)
                bottomButton.text = "TỪ CHỐI"
                bottomButton.setOnClickListener {
                    Speaker.stop()
                    CallManager.decline(call)
                }
                bottomButton.setOnLongClickListener(null)
            }
            else -> {
                statusView.text = statusText(call)
                val speaker = CallManager.isSpeakerOn()
                topButton.text = if (speaker) "LOA NGOÀI: ĐANG BẬT" else "LOA NGOÀI: ĐANG TẮT"
                BigUi.setButtonColor(topButton, if (speaker) BigUi.BLUE else BigUi.GREY)
                topButton.setOnClickListener {
                    val on = !CallManager.isSpeakerOn()
                    CallManager.setSpeaker(on)
                }
                topButton.setOnLongClickListener(null)
                bottomButton.text = "GIỮ ĐỂ KẾT THÚC"
                bottomButton.setOnClickListener {
                    lifecycleScope.launch { Speaker.speak("Giữ nút lâu để kết thúc cuộc gọi.") }
                }
                bottomButton.setOnLongClickListener {
                    CallManager.hangup(call)
                    true
                }
            }
        }
    }

    private fun statusText(call: Call): String = when (call.stateCompat()) {
        Call.STATE_DIALING, Call.STATE_CONNECTING -> "Đang gọi..."
        Call.STATE_HOLDING -> "Đang giữ máy"
        Call.STATE_ACTIVE -> {
            val start = call.details.connectTimeMillis
            if (start > 0) {
                val s = ((System.currentTimeMillis() - start) / 1000).coerceAtLeast(0)
                "Đang nói chuyện %d:%02d".format(s / 60, s % 60)
            } else "Đang nói chuyện"
        }
        Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> "Đã kết thúc"
        else -> ""
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val ringing = CallManager.ringingCall()
        if (ringing != null && CallManager.calls().size == 1) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    Speaker.stop()
                    CallManager.answer(ringing)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    Speaker.stop()
                    CallManager.decline(ringing)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        fun start(context: Context) {
            val i = Intent(context, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            runCatching { context.startActivity(i) }
        }
    }
}
