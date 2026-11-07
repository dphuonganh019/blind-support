package vn.blindsupport.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.blindsupport.app.call.CallManager
import vn.blindsupport.app.core.AppVolume
import vn.blindsupport.app.core.Beeper
import vn.blindsupport.app.core.Contact
import vn.blindsupport.app.core.ContactsRepo
import vn.blindsupport.app.core.HealthCheck
import vn.blindsupport.app.core.MessageStore
import vn.blindsupport.app.core.Prefs
import vn.blindsupport.app.core.Speaker
import vn.blindsupport.app.core.TextUtils
import vn.blindsupport.app.core.VoiceRecognizer
import vn.blindsupport.app.notify.MessageAnnouncer
import vn.blindsupport.app.voip.VoipCallController

/**
 * Màn hình nghe lệnh. Mở bằng cách giữ phím giảm âm lượng (dịch vụ trợ năng), nút nguồn (Samsung) hoặc chạm biểu tượng app.
 * Mở lên là: đọc cảnh báo (nếu có) → "Mời nói" → bíp → nghe lệnh → làm → tự đóng.
 */
class VoiceCommandActivity : ComponentActivity() {

    private companion object {
        /** Tin nhóm thường rất nhiều: mỗi lần chỉ đọc tối đa chừng này tin gần nhất. */
        const val MAX_GROUP_READ = 10
    }

    private lateinit var statusView: TextView
    private var sessionJob: Job? = null
    private var lastWarningAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BigUi.showOverLockScreen(this)
        VoiceRecognizer.preload(applicationContext, lifecycleScope)
        val root = BigUi.column(this)
        statusView = BigUi.title(this, 28f).apply { text = "Đang chuẩn bị..." }
        val again: Button = BigUi.bigButton(this, BigUi.GREEN).apply {
            text = "NÓI LỆNH"
            setOnClickListener { startSession() }
        }
        val close: Button = BigUi.bigButton(this, BigUi.GREY).apply {
            text = "ĐÓNG"
            setOnClickListener { finish() }
        }
        root.addView(statusView)
        root.addView(again)
        root.addView(close)
        setContentView(root)
        BigUi.fitSystemBars(root, activity = this)
    }

    override fun onResume() {
        super.onResume()
        startSession()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Mở lại khi đang hiện (ví dụ giữ phím âm lượng lần nữa): bắt đầu phiên nghe mới.
        startSession()
    }

    /** Phím giảm âm lượng (khi trợ năng không giữ phím lại): dừng đọc. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && sessionJob?.isActive == true) {
            stopReading()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun stopReading() {
        if (sessionJob?.isActive != true) return
        sessionJob?.cancel()
        Speaker.stop()
        lifecycleScope.launch {
            Speaker.speak("Đã dừng.")
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isChangingConfigurations) {
            sessionJob?.cancel()
            Speaker.stop()
        }
    }

    override fun onDestroy() {
        VoiceSession.active = false
        VoiceSession.onStopRequested = null
        super.onDestroy()
    }

    private fun startSession() {
        if (sessionJob?.isActive == true) return
        // Đang có cuộc gọi đổ chuông: nhường cho màn hình cuộc gọi.
        if (CallManager.ringingCall() != null || VoipCallController.isRinging()) {
            finish()
            return
        }
        sessionJob = lifecycleScope.launch {
            VoiceSession.active = true
            VoiceSession.onStopRequested = { runOnUiThread { stopReading() } }
            Speaker.stop()
            try {
                runSession()
            } finally {
                VoiceSession.active = false
                VoiceSession.onStopRequested = null
            }
        }
    }

    private suspend fun runSession() {
        // Cảnh báo thiếu quyền (tối đa mỗi 10 phút một lần cho đỡ phiền).
        val now = System.currentTimeMillis()
        if (now - lastWarningAt > 10 * 60_000L) {
            HealthCheck.spokenProblems(this)?.let {
                lastWarningAt = now
                status("Thiếu quyền – cần người hỗ trợ")
                Speaker.speak(it)
            }
        }

        val contacts = withContext(Dispatchers.IO) { ContactsRepo.allContacts(this@VoiceCommandActivity) }
        val phrases = CommandParser.phrases(contacts)
        val greeting = "Mời nói lệnh sau tiếng bíp."

        var command: Command? = null
        for (attempt in 0 until 2) {
            status("Đang nghe...")
            Speaker.speak(if (attempt == 0) greeting else "Tôi chưa nghe rõ. Mời nói lại sau tiếng bíp.")
            Beeper.beep()
            val heard = VoiceRecognizer.listen(this, phrases, 8_000) { CommandParser.parse(it, contacts) != null }
            if (heard == null && VoiceRecognizer.lastError != null && !VoiceRecognizer.isModelLoaded) {
                Speaker.speak("Bộ nhận dạng giọng nói chưa sẵn sàng. Hãy thử lại sau ít phút.")
                finish()
                return
            }
            command = heard?.let { CommandParser.parse(it, contacts) }
            if (command != null) break
        }
        if (command == null) {
            Speaker.speak("Chưa nghe được lệnh. Nói: hướng dẫn, để nghe các lệnh.")
            finish()
            return
        }
        execute(command, contacts)
        finish()
    }

    private suspend fun execute(command: Command, contacts: List<Contact>) {
        when (command) {
            is Command.CallContact -> confirmAndCall(command.contact.name, command.contact.number)
            is Command.CallNumber -> callByNumber(command.digits)
            Command.CallBack -> {
                val last = withContext(Dispatchers.IO) { ContactsRepo.lastIncomingNumber(this@VoiceCommandActivity) }
                val number = last?.second ?: Prefs.lastMissedNumber
                if (number == null) {
                    Speaker.speak("Không tìm thấy cuộc gọi nào gần đây để gọi lại.")
                } else {
                    val name = last?.first ?: Prefs.lastMissedName ?: ("số " + TextUtils.speakableNumber(number))
                    confirmAndCall(name, number)
                }
            }
            Command.ReadMessages -> readMessages()
            Command.ReadGroupMessages -> readGroupMessages()
            Command.MissedCalls -> {
                val list = withContext(Dispatchers.IO) { ContactsRepo.recentMissedCalls(this@VoiceCommandActivity) }
                if (list.isEmpty()) {
                    Speaker.speak("Không có cuộc gọi nhỡ nào trong 24 giờ qua.")
                } else {
                    val parts = list.take(5).joinToString(". ") {
                        val who = it.name ?: ("số " + TextUtils.speakableNumber(it.number))
                        "$who, ${TextUtils.shortWhen(it.time)}"
                    }
                    Speaker.speak("Có ${list.size} cuộc gọi nhỡ. $parts.")
                }
            }
            Command.Time -> Speaker.speak(TextUtils.timeSentence())
            Command.Battery -> {
                val bm = getSystemService(BatteryManager::class.java)
                val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                val charging = bm?.isCharging == true
                Speaker.speak(
                    if (pct < 0) "Không đọc được mức pin."
                    else "Pin còn $pct phần trăm" + if (charging) ", đang sạc." else "."
                )
            }
            Command.VolumeUp -> Speaker.speak(AppVolume.louder(this))
            Command.VolumeDown -> Speaker.speak(AppVolume.softer(this))
            Command.SpeakSlower -> {
                Prefs.speechRate = (Prefs.speechRate - 0.15f).coerceAtLeast(0.5f)
                Speaker.speak("Tôi sẽ nói chậm hơn như thế này.")
            }
            Command.SpeakFaster -> {
                Prefs.speechRate = (Prefs.speechRate + 0.15f).coerceAtMost(1.8f)
                Speaker.speak("Tôi sẽ nói nhanh hơn như thế này.")
            }
            Command.Help -> Speaker.speak(CommandParser.HELP)
            Command.ReadContacts -> {
                if (contacts.isEmpty()) {
                    Speaker.speak("Danh bạ đang trống, hoặc ứng dụng chưa có quyền đọc danh bạ.")
                } else {
                    val names = contacts.take(40).joinToString(", ") { TextUtils.cleanForSpeech(it.name) }
                    Speaker.speak("Danh bạ có ${contacts.size} người: $names.")
                }
            }
            Command.Cancel -> Speaker.speak("Vâng.")
        }
    }

    private suspend fun readMessages() {
        val unread = MessageStore.unread()
        if (unread.isEmpty()) {
            val latest = MessageStore.latest(3)
            if (latest.isEmpty()) {
                Speaker.speak("Không có tin nhắn nào.")
                return
            }
            Speaker.speak("Không có tin nhắn mới. Đây là ${latest.size} tin gần nhất.")
            for (m in latest.reversed()) {
                status(m.sender)
                Speaker.speak(TextUtils.shortWhen(m.time) + ". " + MessageAnnouncer.format(m))
                delay(300)
            }
            return
        }
        Speaker.speak("Có ${unread.size} tin nhắn mới.")
        for (m in unread) {
            status(m.sender)
            Speaker.speak(MessageAnnouncer.format(m))
            MessageStore.markRead(listOf(m))
            delay(300)
        }
        Speaker.speak("Hết tin nhắn mới.")
        val groups = MessageStore.unreadGroupCount()
        if (groups > 0) Speaker.speak("Có $groups tin nhắn nhóm chưa nghe. Muốn nghe thì nói: tin nhóm.")
    }

    private suspend fun readGroupMessages() {
        val unread = MessageStore.unreadGroups()
        if (unread.isEmpty()) {
            Speaker.speak("Không có tin nhắn nhóm mới.")
            return
        }
        val list = unread.takeLast(MAX_GROUP_READ)
        // Tin cũ hơn số tin sẽ đọc: coi như đã nghe để lần sau không đọc lại từ đầu.
        MessageStore.markRead(unread.dropLast(list.size))
        Speaker.speak(
            if (unread.size > list.size) "Có ${unread.size} tin nhắn nhóm mới. Đọc ${list.size} tin gần nhất. " +
                "Bấm phím giảm âm lượng để dừng."
            else "Có ${unread.size} tin nhắn nhóm mới. Bấm phím giảm âm lượng để dừng."
        )
        for (m in list) {
            status(m.sender)
            Speaker.speak(MessageAnnouncer.format(m))
            MessageStore.markRead(listOf(m))
            delay(300)
        }
        Speaker.speak("Hết tin nhắn nhóm.")
    }

    private suspend fun confirmAndCall(name: String, number: String) {
        val spokenName = TextUtils.cleanForSpeech(name).ifBlank { "số " + TextUtils.speakableNumber(number) }
        status("Gọi $name?")
        for (attempt in 0 until 2) {
            Speaker.speak(
                if (attempt == 0) "Gọi cho $spokenName, phải không? Hãy nói: đúng, hoặc: không, sau tiếng bíp."
                else "Hãy nói: đúng, hoặc: không, sau tiếng bíp."
            )
            Beeper.beep()
            val ans = VoiceRecognizer.listen(this, Confirm.PHRASES, 7_000) { Confirm.parse(it) != null } ?: continue
            if (CommandParser.isYes(ans)) {
                placeCall(number, spokenName)
                return
            }
            if (CommandParser.isNo(ans)) {
                Speaker.speak("Đã hủy.")
                return
            }
        }
        Speaker.speak("Đã hủy cuộc gọi.")
    }

    /** Gọi theo số đọc bằng giọng nói. [initial]: phần số đã đọc kèm trong câu "gọi số ...". */
    private suspend fun callByNumber(initial: String) {
        var digits = initial
        for (attempt in 0 until 2) {
            if (digits.length < 9) {
                if (digits.isEmpty() || attempt > 0) {
                    status("Hãy đọc số điện thoại")
                    Speaker.speak(
                        if (attempt == 0) "Hãy đọc từng số của số điện thoại sau tiếng bíp. Đọc xong thì im lặng chờ."
                        else "Hãy đọc lại số điện thoại sau tiếng bíp."
                    )
                    Beeper.beep()
                    digits = VoiceRecognizer.listenDigits(this)
                } else {
                    // Đã đọc một phần ngay trong câu lệnh: nghe tiếp phần còn lại, không cần hỏi.
                    digits = VoiceRecognizer.listenDigits(this, initial = digits, silenceMs = 2_500)
                }
            }
            status(digits)
            if (digits.length < 3) {
                Speaker.speak("Tôi chưa nghe được số nào.")
                digits = ""
                continue
            }
            val spoken = TextUtils.speakableNumber(digits)
            Speaker.speak("Số bạn đọc là: $spoken. Gọi số này, phải không? Hãy nói: đúng, hoặc: không, sau tiếng bíp.")
            Beeper.beep()
            val ans = VoiceRecognizer.listen(this, Confirm.PHRASES, 7_000) { Confirm.parse(it) != null }
            when {
                ans != null && CommandParser.isYes(ans) -> {
                    placeCall(digits, "số $spoken")
                    return
                }
                attempt == 0 -> digits = "" // nói "không" hoặc im lặng: cho đọc lại một lần
                else -> Unit
            }
        }
        Speaker.speak("Đã hủy cuộc gọi.")
    }

    private suspend fun placeCall(number: String, spokenName: String) {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Speaker.speak("Ứng dụng chưa có quyền gọi điện. Hãy nhờ người hỗ trợ.")
            return
        }
        Speaker.speak("Đang gọi $spokenName.")
        try {
            val tm = getSystemService(TelecomManager::class.java)
            tm.placeCall(Uri.fromParts("tel", number, null), Bundle())
        } catch (e: Exception) {
            Speaker.speak("Không gọi được.")
        }
    }

    private fun status(text: String) {
        statusView.text = text
    }
}
