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
 * - Đang đổ chuông: nửa trên NGHE (xanh), nửa dưới TỪ CHỐI (đỏ). Phím tăng âm lượng = nghe,
 *   phím giảm âm lượng = chỉ tắt chuông (KHÔNG từ chối, tránh bấm nhầm khi muốn vặn nhỏ).
 * - Đang gọi: nửa trên bật/tắt loa, nửa dưới GIỮ để kết thúc (giữ lâu để tránh chạm nhầm).
 */
class InCallActivity : ComponentActivity() {

    private lateinit var nameView: TextView
    private lateinit var statusView: TextView
    private lateinit var topButton: Button
    private lateinit var bottomButton: Button
    private lateinit var keypadButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BigUi.showOverLockScreen(this)

        val root = BigUi.column(this)
        nameView = BigUi.title(this, 34f)
        statusView = BigUi.title(this, 22f)
        topButton = BigUi.bigButton(this, BigUi.GREEN)
        bottomButton = BigUi.bigButton(this, BigUi.RED)
        // Bàn phím trong cuộc gọi (bấm phím khi gọi tổng đài). Nhỏ hơn hai nút chính.
        keypadButton = BigUi.bigButton(this, BigUi.GREY).apply {
            text = "BÀN PHÍM"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
            layoutParams = (layoutParams as android.widget.LinearLayout.LayoutParams).apply { weight = 0.45f }
            setOnClickListener { CallManager.primaryCall()?.let { showDtmfPad(it) } }
        }
        root.addView(nameView)
        root.addView(statusView)
        root.addView(topButton)
        root.addView(keypadButton)
        root.addView(bottomButton)
        setContentView(root)
        BigUi.fitSystemBars(root, activity = this)

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
                keypadButton.visibility = android.view.View.GONE
                statusView.text = "Cuộc gọi đến"
                topButton.text = "NGHE"
                BigUi.setButtonColor(topButton, BigUi.GREEN)
                topButton.setOnClickListener {
                    Speaker.stop()
                    CallManager.answer(call)
                }
                topButton.setOnLongClickListener(null)
                // Từ chối phải GIỮ nút: người khiếm thị cầm máy dễ chạm nhầm nửa dưới màn hình.
                bottomButton.text = "GIỮ ĐỂ TỪ CHỐI"
                bottomButton.setOnClickListener(null)
                bottomButton.setOnLongClickListener {
                    Speaker.stop()
                    CallManager.decline(call)
                    true
                }
            }
            else -> {
                keypadButton.visibility =
                    if (call.stateCompat() == Call.STATE_ACTIVE) android.view.View.VISIBLE else android.view.View.GONE
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

    private fun showDtmfPad(call: Call) {
        val typed = StringBuilder()
        val display = android.widget.TextView(this).apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 30f)
            gravity = android.view.Gravity.CENTER
            text = " "
        }
        val grid = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val p = BigUi.dp(this@InCallActivity, 8)
            setPadding(p, p, p, p)
            addView(display)
        }
        for (row in listOf("123", "456", "789", "*0#")) {
            val r = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.HORIZONTAL }
            for (ch in row) {
                r.addView(Button(this).apply {
                    text = ch.toString()
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 28f)
                    setOnClickListener {
                        typed.append(ch)
                        display.text = typed
                        runCatching { call.playDtmfTone(ch) }
                        postDelayed({ runCatching { call.stopDtmfTone() } }, 180)
                    }
                }, android.widget.LinearLayout.LayoutParams(0, BigUi.dp(this@InCallActivity, 72), 1f))
            }
            grid.addView(r)
        }
        android.app.AlertDialog.Builder(this)
            .setView(grid)
            .setPositiveButton("Đóng", null)
            .show()
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
                    // Chỉ tắt chuông và ngừng hỏi, không từ chối (dễ bấm nhầm khi muốn vặn nhỏ).
                    Speaker.stop()
                    CallManager.service?.silenceRinging()
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
