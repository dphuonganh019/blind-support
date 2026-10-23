package vn.blindsupport.app.voip

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import vn.blindsupport.app.ui.BigUi

/**
 * Màn hình to cho cuộc gọi Zalo/WhatsApp đang đổ chuông. Việc màn hình này hiện lên còn giúp app
 * được phép dùng micro để nghe câu trả lời "nghe"/"không".
 */
class VoipCallActivity : ComponentActivity() {

    private lateinit var nameView: TextView
    private lateinit var statusView: TextView
    private lateinit var answerButton: Button
    private lateinit var declineButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BigUi.showOverLockScreen(this)
        val root = BigUi.column(this)
        nameView = BigUi.title(this, 34f)
        statusView = BigUi.title(this, 22f)
        answerButton = BigUi.bigButton(this, BigUi.GREEN).apply {
            text = "NGHE"
            setOnClickListener { VoipCallController.answer(this@VoipCallActivity) }
        }
        declineButton = BigUi.bigButton(this, BigUi.RED).apply {
            text = "TỪ CHỐI"
            setOnClickListener { VoipCallController.decline(this@VoipCallActivity) }
        }
        root.addView(nameView)
        root.addView(statusView)
        root.addView(answerButton)
        root.addView(declineButton)
        setContentView(root)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                VoipCallController.state.collect { st ->
                    if (st != VoipCallController.State.RINGING) {
                        finish()
                    } else {
                        nameView.text = VoipCallController.current?.callerName ?: ""
                        statusView.text = "Cuộc gọi ${VoipCallController.appName()} đến"
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VoipCallController.isRinging()) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    VoipCallController.answer(this)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    VoipCallController.decline(this)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        fun start(context: Context) {
            val i = Intent(context, VoipCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            runCatching { context.startActivity(i) }
        }
    }
}
