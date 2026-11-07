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
import vn.blindsupport.app.core.AppCallFinder
import vn.blindsupport.app.core.Contact
import vn.blindsupport.app.core.DigitWords
import vn.blindsupport.app.core.ContactsRepo
import vn.blindsupport.app.core.HealthCheck
import vn.blindsupport.app.core.MessageStore
import vn.blindsupport.app.core.Prefs
import vn.blindsupport.app.core.Speaker
import vn.blindsupport.app.core.SpeechInput
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
        /** Gợi ý cho bộ nhận dạng Google khi hỏi "đúng không" (Android 13+): nghe "đúng" chuẩn hơn. */
        val CONFIRM_HINTS = listOf("đúng", "đúng rồi", "không", "vâng", "có")
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
        nameHints = contacts.map { it.name }
        val greeting = "Mời nói lệnh sau tiếng bíp."

        var command: Command? = null
        for (attempt in 0 until 2) {
            status("Đang nghe...")
            Speaker.speak(if (attempt == 0) greeting else "Tôi chưa nghe rõ. Mời nói lại sau tiếng bíp.")
            Beeper.beep()
            val heard = hear(phrases)
            if (heard.isEmpty() && SpeechInput.lastEngine == "offline" &&
                VoiceRecognizer.lastError != null && !VoiceRecognizer.isModelLoaded
            ) {
                Speaker.speak("Bộ nhận dạng giọng nói chưa sẵn sàng. Hãy thử lại sau ít phút.")
                finish()
                return
            }
            // Bộ nhận dạng Google trả về vài cách nghe: lấy cách đầu tiên hiểu được thành lệnh.
            command = heard.firstNotNullOfOrNull { CommandParser.parse(it, contacts) }
            logHeard(heard, command != null)
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
            is Command.ChooseContact -> chooseAndCall(command.candidates)
            is Command.TooManyContacts -> askWhoToCall(
                "Có ${command.count} người có tên ${command.spoken}. Hãy nói thêm tên, ví dụ tên đầy đủ, sau tiếng bíp.",
                contacts,
            )
            is Command.ContactNotFound -> askWhoToCall(
                "Không tìm thấy ${command.spoken} trong danh bạ. Hãy nói lại tên, hoặc đọc số điện thoại, sau tiếng bíp.",
                contacts,
            )
            is Command.AppCall -> {
                viaApp = command.app
                try {
                    execute(command.inner, contacts)
                } finally {
                    viaApp = null
                }
            }
            Command.CallWho -> askWhoToCall(
                "Bạn muốn gọi ai? Hãy nói tên trong danh bạ, hoặc đọc số điện thoại, sau tiếng bíp.",
                contacts,
            )
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

    /**
     * Hỏi lại tên (hoặc số) người cần gọi, tối đa 2 lần. Dùng khi chỉ nói "gọi", khi tên không có
     * trong danh bạ, hoặc khi quá nhiều người trùng tên.
     */
    private suspend fun askWhoToCall(question: String, contacts: List<Contact>) {
        val phrases = CommandParser.namePhrases(contacts) + DigitWords.WORDS + listOf("số", "hủy", "thôi")
        var prompt = question
        for (attempt in 0 until 2) {
            status("Gọi ai?")
            Speaker.speak(prompt)
            Beeper.beep()
            val heard = hear(phrases)
            if (heard.isEmpty()) {
                prompt = "Tôi chưa nghe rõ. Hãy nói tên, hoặc đọc số điện thoại, sau tiếng bíp."
                continue
            }
            if (heard.any { TextUtils.normalize(it) in setOf("hủy", "thôi", "không") }) {
                logHeard(heard, true)
                Speaker.speak("Đã hủy.")
                return
            }
            val cmds = heard.map { h ->
                val n = TextUtils.normalize(h)
                CommandParser.parse(if (n.startsWith("gọi")) n else "gọi $n", contacts)
            }
            val cmd = cmds.firstOrNull {
                it is Command.CallContact || it is Command.ChooseContact || it is Command.CallNumber
            } ?: cmds.firstOrNull()
            logHeard(heard, cmd is Command.CallContact || cmd is Command.ChooseContact || cmd is Command.CallNumber)
            when (cmd) {
                is Command.CallContact -> return confirmAndCall(cmd.contact.name, cmd.contact.number)
                is Command.ChooseContact -> return chooseAndCall(cmd.candidates)
                is Command.CallNumber -> return callByNumber(cmd.digits)
                is Command.TooManyContacts ->
                    prompt = "Có ${cmd.count} người có tên ${cmd.spoken}. Hãy nói thêm tên sau tiếng bíp."
                else ->
                    prompt = "Không tìm thấy tên này trong danh bạ. Hãy nói lại tên, hoặc đọc số điện thoại, sau tiếng bíp."
            }
        }
        Speaker.speak("Đã hủy cuộc gọi.")
    }

    /**
     * Nhiều người khớp tên: đọc danh sách có số thứ tự, người dùng nói số thứ tự để gọi ngay.
     * Tên trùng nhau thì đọc kèm 3 số cuối để phân biệt.
     */
    private suspend fun chooseAndCall(candidates: List<Contact>) {
        val names = candidates.map { TextUtils.cleanForSpeech(it.name).ifBlank { "không tên" } }
        val labels = candidates.mapIndexed { i, c ->
            val dup = names.count { it.equals(names[i], ignoreCase = true) } > 1
            if (dup) "${names[i]}, số đuôi ${c.number.filter(Char::isDigit).takeLast(3).toCharArray().joinToString(" ")}"
            else names[i]
        }
        val list = labels.mapIndexed { i, l -> "Số ${i + 1}: $l." }.joinToString(" ")
        status(labels.mapIndexed { i, l -> "${i + 1}. $l" }.joinToString("\n"))
        var prompt = "Có ${candidates.size} người. $list Bạn muốn gọi ai? Hãy nói số thứ tự sau tiếng bíp. Nói: hủy, để thôi."
        for (attempt in 0 until 2) {
            Speaker.speak(prompt)
            Beeper.beep()
            val ans = hear(Choice.phrases(), hints = emptyList())
            val picked = ans.firstNotNullOfOrNull { Choice.parse(it, candidates.size) }
            logHeard(ans, picked != null)
            when (val choice = picked) {
                is Choice.Pick -> {
                    val c = candidates[choice.index]
                    placeCall(c.number, labels[choice.index])
                    return
                }
                Choice.Cancel -> {
                    Speaker.speak("Đã hủy.")
                    return
                }
                null -> {
                    prompt = "Tôi chưa nghe rõ. Hãy nói một số từ 1 đến ${candidates.size}. $list"
                }
            }
        }
        Speaker.speak("Đã hủy cuộc gọi.")
    }

    private suspend fun confirmAndCall(name: String, number: String) {
        val spokenName = TextUtils.cleanForSpeech(name).ifBlank { "số " + TextUtils.speakableNumber(number) }
        status("Gọi $name?")
        for (attempt in 0 until 2) {
            Speaker.speak(
                if (attempt == 0) "Bạn muốn gọi ${viaApp?.let { "$it " } ?: ""}cho $spokenName, đúng không? " +
                    "Nói: đúng, để gọi. Nói: không, để hủy."
                else "Hãy nói: đúng, hoặc: không, sau tiếng bíp."
            )
            Beeper.beep()
            val ans = hear(Confirm.PHRASES, hints = CONFIRM_HINTS)
            val answer = ans.firstNotNullOfOrNull { Confirm.parse(it) }
            logHeard(ans, answer != null)
            when (answer) {
                Confirm.YES -> {
                    placeCall(number, spokenName)
                    return
                }
                Confirm.NO -> {
                    Speaker.speak("Đã hủy.")
                    return
                }
                null -> Unit
            }
        }
        Speaker.speak("Đã hủy cuộc gọi.")
    }

    /**
     * Gọi theo số đọc bằng giọng nói. [initial]: phần số đã đọc kèm trong câu "gọi …".
     * Đọc lại số để xác nhận: "đúng" thì gọi ngay, "không" thì hủy.
     */
    private suspend fun callByNumber(initial: String) {
        var digits = initial
        if (digits.length < 3) {
            status("Hãy đọc số điện thoại")
            Speaker.speak("Hãy đọc số điện thoại sau tiếng bíp. Đọc xong thì im lặng chờ.")
            Beeper.beep()
            val heard = hear(DigitWords.WORDS + listOf("số", "xong"), hints = emptyList())
            digits = heard.map { DigitWords.toDigits(it) }.firstOrNull { it.length >= 3 } ?: digits
            logHeard(heard, digits.length >= 3)
        }
        status(digits)
        if (digits.length < 3) {
            Speaker.speak("Tôi chưa nghe được số điện thoại. Đã hủy cuộc gọi.")
            return
        }
        val spoken = TextUtils.speakableNumber(digits)
        for (attempt in 0 until 2) {
            Speaker.speak(
                if (attempt == 0) "Bạn muốn gọi số $spoken, đúng không? Nói: đúng, để thực hiện cuộc gọi. Nói: không, để hủy."
                else "Số $spoken. Hãy nói: đúng, hoặc: không, sau tiếng bíp."
            )
            Beeper.beep()
            val ans = hear(Confirm.PHRASES, hints = CONFIRM_HINTS)
            val answer = ans.firstNotNullOfOrNull { Confirm.parse(it) }
            logHeard(ans, answer != null)
            when (answer) {
                Confirm.YES -> {
                    placeCall(digits, "số $spoken")
                    return
                }
                Confirm.NO -> {
                    Speaker.speak("Đã hủy.")
                    return
                }
                null -> Unit
            }
        }
        Speaker.speak("Đã hủy cuộc gọi.")
    }

    private suspend fun placeCall(number: String, spokenName: String) {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Speaker.speak("Ứng dụng chưa có quyền gọi điện. Hãy nhờ người hỗ trợ.")
            return
        }
        viaApp?.let { app ->
            if (tryAppCall(app, number, spokenName)) return
            Speaker.speak("Không tìm thấy cách gọi $app cho người này. Tôi sẽ gọi điện thường bằng sim.")
        }
        Speaker.speak("Đang gọi $spokenName.")
        try {
            val tm = getSystemService(TelecomManager::class.java)
            tm.placeCall(Uri.fromParts("tel", TextUtils.dialableNumber(number), null), Bundle())
        } catch (e: Exception) {
            Speaker.speak("Không gọi được.")
        }
    }

    /** Đang xử lý lệnh "gọi Zalo/WhatsApp …": tên ứng dụng, hoặc null nếu gọi sim. */
    private var viaApp: String? = null

    /**
     * Gọi qua Zalo/WhatsApp bằng dòng "gọi" mà ứng dụng đó đồng bộ vào danh bạ. Trả về false nếu
     * người này không có dòng như vậy (hoặc mở không được).
     */
    private suspend fun tryAppCall(app: String, number: String, spokenName: String): Boolean {
        val key = if (app == "Zalo") "zalo" else "whatsapp"
        val pkg = if (app == "Zalo") "com.zing.zalo" else "com.whatsapp"
        val (action, diag) = withContext(Dispatchers.IO) {
            AppCallFinder.find(this@VoiceCommandActivity, key, number, spokenName)
        }
        VoiceRecognizer.log("[$app] $spokenName: $diag", action != null)
        if (action == null) return false
        Speaker.speak("Đang gọi $app cho $spokenName.")
        return try {
            val uri = android.content.ContentUris.withAppendedId(android.provider.ContactsContract.Data.CONTENT_URI, action.dataId)
            startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, action.mimeType).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            VoipCallController.onOutgoingStarted(applicationContext, pkg)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Tên trong danh bạ, giúp bộ nhận dạng Google nghe đúng tên khi gọi. */
    private var nameHints: List<String> = emptyList()

    /** Nghe một câu (Google nếu có, không thì offline). Trả về các cách nghe, tốt nhất trước. */
    private suspend fun hear(phrases: Collection<String>, hints: List<String> = nameHints): List<String> {
        status("Mời nói...")
        return SpeechInput.listen(this, phrases, hints)
    }

    /** Ghi nhật ký và hiện chữ nghe được lên màn hình (cho người hỗ trợ xem). */
    private fun logHeard(heard: List<String>, ok: Boolean) {
        val first = heard.firstOrNull() ?: return
        VoiceRecognizer.log("[${SpeechInput.lastEngine}] " + heard.joinToString(" | "), ok)
        status("Nghe được: \"$first\"")
    }

    private fun status(text: String) {
        statusView.text = text
    }
}
