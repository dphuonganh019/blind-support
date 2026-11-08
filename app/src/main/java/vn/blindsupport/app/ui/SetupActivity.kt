package vn.blindsupport.app.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.app.AlertDialog
import android.content.Intent
import vn.blindsupport.app.core.AppControl
import android.content.res.ColorStateList
import android.widget.Toast
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.blindsupport.app.core.ContactsRepo
import vn.blindsupport.app.App
import vn.blindsupport.app.core.HealthCheck
import vn.blindsupport.app.core.Prefs
import vn.blindsupport.app.core.Speaker
import vn.blindsupport.app.core.VoiceCallPrompt
import vn.blindsupport.app.core.VoiceRecognizer
import vn.blindsupport.app.notify.BlindNotificationListener

/**
 * Màn hình dành cho NGƯỜI HỖ TRỢ (người sáng mắt) khi cài đặt máy lần đầu:
 * cấp quyền, kiểm tra, thử giọng đọc / nhận lệnh, chỉnh các tùy chọn.
 */
class SetupActivity : ComponentActivity() {

    private lateinit var checks: LinearLayout
    private lateinit var unknownNames: LinearLayout

    /**
     * Liệt kê tên danh bạ có chữ mà bộ nhận dạng giọng nói không biết (ví dụ tên tiếng Anh):
     * những người này không gọi được bằng giọng nói. Người hỗ trợ nên đổi tên trong danh bạ.
     */
    private val levelButtons = HashMap<Int, Button>()
    /** Màu chữ gốc của từng nút mức (để trả lại khi nút không còn được chọn). */
    private val levelDefaultTextColors = HashMap<Int, ColorStateList>()

    /** Nút mức đang chọn: màu xanh, có dấu ✓. */
    private fun renderLevelButtons() {
        for ((level, title) in LEVELS) {
            val b = levelButtons[level] ?: continue
            val active = Prefs.voiceVolume == 100 && Prefs.boostLevel == level
            b.text = if (active) "✓ $title" else title
            // Không đụng tới "tint" của nút: trên Samsung, đổi tint xong là nút mất màu nền gốc
            // (nền trắng, chữ trắng). Chỉ phủ màu xanh lên nút đang chọn, nút khác giữ nguyên.
            b.background?.colorFilter = if (active) PorterDuffColorFilter(GREEN, PorterDuff.Mode.SRC_IN) else null
            levelDefaultTextColors[level]?.let { b.setTextColor(if (active) ColorStateList.valueOf(Color.WHITE) else it) }
        }
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun ringFirstLabel(): String =
        "Đổ chuông trước khi đọc tên người gọi: ${Prefs.ringFirstSeconds} giây (bấm để đổi)"

    private fun renderUnknownNames() {
        unknownNames.removeAllViews()
        lifecycleScope.launch {
            val bad = withContext(Dispatchers.IO) {
                ContactsRepo.allContacts(this@SetupActivity).mapNotNull { c ->
                    val unknown = VoiceRecognizer.unknownWords(this@SetupActivity, c.normalizedName)
                    if (unknown.isEmpty()) null else c.name to unknown
                }
            }
            if (bad.isEmpty()) return@launch
            unknownNames.addView(note(
                "⚠️ ${bad.size} tên trong danh bạ KHÔNG gọi được bằng giọng nói vì bộ nhận dạng không biết chữ " +
                    "(thường là tên tiếng Anh, tên viết tắt). Hãy đổi tên trong danh bạ sang chữ tiếng Việt đơn giản, " +
                    "ví dụ \"Chloe\" → \"Con Gái\":"
            ))
            for ((name, words) in bad.take(30)) {
                unknownNames.addView(note("   • $name  (không biết: ${words.joinToString(", ")})"))
            }
            if (bad.size > 30) unknownNames.addView(note("   … và ${bad.size - 30} tên khác."))
        }
    }
    private lateinit var loudnessLabel: TextView

    private fun renderLoudness() {
        loudnessLabel.text = "Hiện tại: âm lượng ${Prefs.voiceVolume}%, khuếch đại mức ${Prefs.boostLevel}, " +
            "tốc độ đọc ${"%.2f".format(Prefs.speechRate)}"
    }

    private val runtimePermissions: Array<String>
        get() = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.ANSWER_PHONE_CALLS)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.WHITE) }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(16)
            setPadding(p, p, p, p)
        }
        scroll.addView(col)
        setContentView(scroll)
        BigUi.fitSystemBars(scroll, lightBars = true, activity = this)

        col.addView(heading("Cài đặt Trợ Lý Nghe Gọi"))
        col.addView(note("Phiên bản ${versionName()}. Màn hình này dành cho người hỗ trợ. Hãy làm cho tới khi tất cả các mục đều có dấu ✅."))
        offBanner = TextView(this).apply {
            text = "⚠️ Trợ Lý đang TẠM TẮT: không đọc, không hỏi, không bấm gì. Kéo xuống cuối trang, bấm \"Bật lại Trợ Lý\" để dùng lại."
            setTextColor(Color.WHITE)
            setBackgroundColor(RED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            val p = dp(12)
            setPadding(p, p, p, p)
        }
        col.addView(offBanner)

        col.addView(heading("1. Quyền và trạng thái"))
        checks = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(checks)
        unknownNames = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(unknownNames)
        col.addView(button("Cấp tất cả quyền cơ bản") { requestPermissions(runtimePermissions, 10) })
        col.addView(note(
            "Nếu Samsung không cho bật \"Quyền đọc thông báo\" hoặc \"Hỗ trợ tiếp cận\" (báo là cài đặt bị hạn chế): " +
                "mở Thông tin ứng dụng → bấm dấu ⋮ góc trên → \"Cho phép cài đặt hạn chế\", rồi bật lại."
        ))
        col.addView(button("Mở Thông tin ứng dụng") {
            open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        })

        col.addView(heading("2. Thử nghiệm"))
        col.addView(button("Thử giọng đọc") {
            lifecycleScope.launch { Speaker.speak("Xin chào. Đây là giọng đọc của Trợ Lý Nghe Gọi.") }
        })
        col.addView(heading("Độ to giọng đọc"))
        loudnessLabel = note("")
        col.addView(loudnessLabel)
        col.addView(note(
            "Giọng đọc dùng thanh âm lượng \"Chuông báo\". App tự đặt lại thanh này trước mỗi lần đọc, " +
                "rồi khuếch đại thêm bằng phần mềm. Mức 3 hợp với người nghe kém; mức 4 to nhất nhưng có thể hơi rè."
        ))
        for ((level, title) in LEVELS) {
            val b = button(title) {
                Prefs.voiceVolume = 100
                Prefs.boostLevel = level
                renderLoudness()
                renderLevelButtons()
                toast("Đã chọn: $title")
                lifecycleScope.launch { Speaker.speak("Mức $level. Có cuộc gọi từ Mẹ. Bạn có muốn nghe không?") }
            }
            levelButtons[level] = b
            b.background?.mutate()
            levelDefaultTextColors[level] = b.textColors
            col.addView(b)
        }
        renderLevelButtons()
        col.addView(button("Đọc chậm hơn") {
            Prefs.speechRate = (Prefs.speechRate - 0.1f).coerceAtLeast(0.5f)
            renderLoudness()
            toast("Đã đổi tốc độ đọc: ${"%.2f".format(Prefs.speechRate)}")
            lifecycleScope.launch { Speaker.speak("Tôi sẽ đọc với tốc độ này.") }
        })
        col.addView(button("Đọc nhanh hơn") {
            Prefs.speechRate = (Prefs.speechRate + 0.1f).coerceAtMost(1.8f)
            renderLoudness()
            toast("Đã đổi tốc độ đọc: ${"%.2f".format(Prefs.speechRate)}")
            lifecycleScope.launch { Speaker.speak("Tôi sẽ đọc với tốc độ này.") }
        })
        renderLoudness()
        col.addView(button("Thử nhận lệnh giọng nói") {
            startActivity(Intent(this, VoiceCommandActivity::class.java))
        })
        col.addView(button("Giả lập cuộc gọi đến (không gọi thật)") { simulateCall() })
        col.addView(button("Gửi nhật ký nhận dạng giọng nói") {
            val log = Prefs.recognitionLog
            if (log.isBlank()) {
                lifecycleScope.launch { Speaker.speak("Chưa có nhật ký. Hãy thử nói vài lệnh trước.") }
            } else {
                val i = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "Trợ Lý Nghe Gọi ${versionName()} - nhật ký nhận dạng")
                    .putExtra(Intent.EXTRA_TEXT, "Mỗi dòng: thời gian, NHẬN/bỏ, chữ app nghe được\n\n$log")
                open(Intent.createChooser(i, "Gửi qua"))
            }
        })
        col.addView(note(
            "Nếu app báo \"chưa bật được loa ngoài\" khi nghe máy Zalo/WhatsApp: ngay sau cuộc gọi đó, " +
                "bấm nút dưới đây và gửi nội dung cho người làm app để sửa."
        ))
        col.addView(button("Gửi thông tin màn hình Zalo (lỗi loa ngoài)") {
            val dump = Prefs.lastUiDump
            if (dump.isBlank()) {
                lifecycleScope.launch { Speaker.speak("Chưa có thông tin. Hãy thử một cuộc gọi Zalo trước.") }
            } else {
                val i = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "Trợ Lý Nghe Gọi ${versionName()} - màn hình Zalo")
                    .putExtra(Intent.EXTRA_TEXT, dump)
                open(Intent.createChooser(i, "Gửi qua"))
            }
        })

        col.addView(note(
            "Nếu cuộc gọi hay tin nhắn Zalo/WhatsApp không được báo, hoặc báo trễ: ngay sau lần đó, " +
                "bấm nút dưới đây và gửi nội dung cho người làm app."
        ))
        col.addView(button("Gửi nhật ký thông báo Zalo / WhatsApp") {
            val log = vn.blindsupport.app.core.Prefs.notifLog
            if (log.isBlank()) {
                lifecycleScope.launch { Speaker.speak("Chưa có nhật ký. Hãy nhờ người gọi hoặc nhắn Zalo thử trước.") }
            } else {
                val head = "Máy: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}\n" +
                    "Zalo: ${runCatching { packageManager.getPackageInfo("com.zing.zalo", 0).versionName }.getOrNull() ?: "?"}\n" +
                    "Mỗi thông báo: giờ app nhận, loại, nút, tiêu đề | nội dung, rồi → việc app đã làm\n\n"
                val i = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "Trợ Lý Nghe Gọi ${versionName()} - nhật ký thông báo")
                    .putExtra(Intent.EXTRA_TEXT, head + log)
                open(Intent.createChooser(i, "Gửi qua"))
            }
        })

        col.addView(heading("3. Tùy chọn"))
        col.addView(toggle("Hỏi \"có muốn nghe không\" và nghe câu trả lời bằng giọng nói", Prefs.voiceAnswer) { Prefs.voiceAnswer = it })
        lateinit var ringButton: Button
        ringButton = button(ringFirstLabel()) {
            val options = listOf(0, 3, 6, 10)
            val next = options[(options.indexOf(Prefs.ringFirstSeconds).coerceAtLeast(0) + 1) % options.size]
            Prefs.ringFirstSeconds = next
            ringButton.text = ringFirstLabel()
            toast("Đổ chuông ${next} giây trước khi đọc tên")
        }
        col.addView(ringButton)
        col.addView(toggle("Rung liên tục khi có cuộc gọi đến", Prefs.vibrateOnCall) { Prefs.vibrateOnCall = it })
        col.addView(toggle("Tự bật loa ngoài khi nghe máy", Prefs.autoSpeaker) { Prefs.autoSpeaker = it })
        col.addView(toggle("Âm lượng cuộc gọi tối đa khi nghe máy", Prefs.maxCallVolume) { Prefs.maxCallVolume = it })
        col.addView(toggle("Báo cuộc gọi Zalo / WhatsApp", Prefs.voipCalls) { Prefs.voipCalls = it })
        col.addView(toggle("Đọc tin nhắn Zalo", Prefs.readZalo) { Prefs.readZalo = it })
        col.addView(toggle("Đọc tin nhắn WhatsApp", Prefs.readWhatsapp) { Prefs.readWhatsapp = it })
        col.addView(toggle("Đọc tin nhắn SMS", Prefs.readSms) { Prefs.readSms = it })
        col.addView(toggle("Đọc to cả tin nhắn NHÓM (thường rất nhiều)", Prefs.readGroups) { Prefs.readGroups = it })
        col.addView(toggle("Đọc nội dung tin (tắt: chỉ đọc tên người gửi)", Prefs.readContent) { Prefs.readContent = it })
        col.addView(toggle("Báo pin yếu / cắm sạc / rút sạc", Prefs.announceBattery) { Prefs.announceBattery = it })
        col.addView(toggle(
            "Giữ phím GIẢM âm lượng khoảng 1,5 giây để mở lệnh giọng nói (cần bật Hỗ trợ tiếp cận)",
            Prefs.volumeKeyTrigger
        ) { Prefs.volumeKeyTrigger = it })
        col.addView(toggle(
            "Giờ yên tĩnh ${Prefs.quietStart}h – ${Prefs.quietEnd}h: không đọc tin nhắn (vẫn báo cuộc gọi)",
            Prefs.quietHours
        ) { Prefs.quietHours = it })

        col.addView(heading("4. Cách mở lệnh giọng nói"))
        col.addView(note(TRIGGER_GUIDE))
        col.addView(heading("5. Cài đặt riêng theo hãng máy (làm một lần)"))
        col.addView(note(if (Build.MANUFACTURER.lowercase().let { it.contains("oppo") || it.contains("realme") || it.contains("oneplus") }) OPPO_GUIDE else SAMSUNG_GUIDE))
        col.addView(button("Mở Thông tin ứng dụng (pin, tự khởi chạy...)") {
            open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        })
        col.addView(button("Mở Cài đặt máy") { open(Intent(Settings.ACTION_SETTINGS)) })

        col.addView(heading("6. Tạm tắt hoặc gỡ Trợ Lý"))
        col.addView(note(
            "Dùng khi app không chạy đúng hoặc làm người dùng khó chịu.\n" +
                "• Tạm tắt hết: app ngừng mọi việc, tự tắt Trợ năng, trả lại âm lượng. Sau đó máy mở màn hình " +
                "chọn ứng dụng điện thoại mặc định: chạm \"Ứng dụng điện thoại\" rồi chọn ứng dụng Điện thoại gốc của máy. " +
                "Có thể bật lại bất cứ lúc nào.\n" +
                "• Gỡ hẳn ứng dụng: máy trở về như chưa cài; Android tự trả lại ứng dụng Điện thoại gốc. " +
                "Sau khi gỡ, nên gọi thử một cuộc vào máy để kiểm tra."
        ))
        switchButton = button("") { onSwitchPressed() }
        col.addView(switchButton)
        col.addView(button("Gỡ hẳn ứng dụng – trả máy như cũ") { confirmUninstall() }.apply {
            backgroundTintList = ColorStateList.valueOf(RED)
            setTextColor(Color.WHITE)
        })
        renderSwitch()
    }

    override fun onResume() {
        super.onResume()
        renderChecks()
        renderSwitch()
    }

    private lateinit var offBanner: TextView
    private lateinit var switchButton: Button

    /** Nút đỏ "Tạm tắt hết" khi đang bật; nút xanh "Bật lại Trợ Lý" khi đang tắt. */
    private fun renderSwitch() {
        if (!::switchButton.isInitialized) return
        val on = Prefs.appEnabled
        switchButton.text = if (on) "Tạm tắt hết" else "Bật lại Trợ Lý"
        switchButton.backgroundTintList = ColorStateList.valueOf(if (on) RED else GREEN)
        switchButton.setTextColor(Color.WHITE)
        offBanner.visibility = if (on) View.GONE else View.VISIBLE
    }

    private fun onSwitchPressed() {
        if (Prefs.appEnabled) {
            AlertDialog.Builder(this)
                .setTitle("Tạm tắt hết Trợ Lý?")
                .setMessage(
                    "App sẽ ngừng đọc tên người gọi, đọc tin nhắn, nghe lệnh giọng nói và tự tắt Trợ năng.\n\n" +
                        "Bước tiếp theo: máy mở màn hình chọn ứng dụng mặc định. Hãy chạm \"Ứng dụng điện thoại\" " +
                        "rồi chọn ứng dụng Điện thoại gốc của máy."
                )
                .setPositiveButton("Tạm tắt") { _, _ ->
                    AppControl.disableAll(this)
                    renderSwitch()
                    renderChecks()
                    toast("Đã tạm tắt Trợ Lý")
                    if (!open(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))) open(Intent(Settings.ACTION_SETTINGS))
                }
                .setNegativeButton("Không", null)
                .show()
        } else {
            AppControl.enableAll(this)
            renderSwitch()
            renderChecks()
            AlertDialog.Builder(this)
                .setTitle("Đã bật lại Trợ Lý")
                .setMessage(
                    "Android bắt buộc người dùng tự làm lại 2 việc: bật Trợ năng cho Trợ Lý Nghe Gọi, và chọn " +
                        "Trợ Lý làm ứng dụng điện thoại mặc định. Hãy bấm \"Sửa\" ở các mục có dấu ❌ ở đầu trang."
                )
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun confirmUninstall() {
        AlertDialog.Builder(this)
            .setTitle("Gỡ hẳn Trợ Lý Nghe Gọi?")
            .setMessage(
                "Máy sẽ trở về như chưa cài: Android tự trả lại ứng dụng Điện thoại gốc, tự xóa mọi quyền và Trợ năng " +
                    "của app. Sau khi gỡ, hãy gọi thử một cuộc vào máy để kiểm tra."
            )
            .setPositiveButton("Gỡ") { _, _ ->
                AppControl.disableAll(this)
                val uri = Uri.fromParts("package", packageName, null)
                if (!open(Intent(Intent.ACTION_DELETE, uri))) {
                    open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
                }
            }
            .setNegativeButton("Không", null)
            .show()
    }

    private fun renderChecks() {
        checks.removeAllViews()
        lifecycleScope.launch {
            VoiceRecognizer.ensureModel(this@SetupActivity)
            Speaker.refreshOfflineVoice()
            checks.removeAllViews()
            for (item in HealthCheck.run(this@SetupActivity) + HealthCheck.deadServices(this@SetupActivity)) {
                val row = LinearLayout(this@SetupActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val label = TextView(this@SetupActivity).apply {
                    text = (if (item.ok) "✅ " else "❌ ") + item.title
                    setTextColor(Color.BLACK)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(label)
                if (!item.ok) {
                    row.addView(Button(this@SetupActivity).apply {
                        text = "Sửa"
                        setOnClickListener { fix(item.key) }
                    })
                }
                checks.addView(row)
            }
            if (!BlindNotificationListener.connected && HealthCheck.isNotificationListenerEnabled(this@SetupActivity)) {
                checks.addView(note("⚠️ Đã cấp quyền đọc thông báo nhưng dịch vụ chưa chạy. Hãy tắt rồi bật lại quyền này, hoặc khởi động lại máy."))
            }
            renderUnknownNames()
        }
    }

    private fun fix(key: String) {
        val pkgUri = Uri.fromParts("package", packageName, null)
        when (key) {
            "dialer" -> {
                val rm = getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    @Suppress("DEPRECATION")
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER), 11)
                } else {
                    open(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                }
            }
            "mic", "contacts", "call", "calllog", "phone", "notif" -> requestPermissions(runtimePermissions, 10)
            "listener" -> {
                val i = if (Build.VERSION.SDK_INT >= 30) {
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        ComponentName(this, BlindNotificationListener::class.java).flattenToString()
                    )
                } else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                if (!open(i)) open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            "a11y", "a11y_dead" -> open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            "listener_dead" -> open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            "overlay" -> open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri))
            "battery" -> {
                @Suppress("BatteryLife")
                val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri)
                if (!open(i)) open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
            "autorevoke" -> {
                if (Build.VERSION.SDK_INT >= 30) {
                    if (!open(Intent(Intent.ACTION_AUTO_REVOKE_PERMISSIONS, pkgUri))) {
                        open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri))
                    }
                }
            }
            "fullscreen" -> {
                if (Build.VERSION.SDK_INT >= 34) {
                    open(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkgUri))
                }
            }
            "tts", "tts_offline" -> {
                if (!open(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))) {
                    open(Intent("com.android.settings.TTS_SETTINGS"))
                }
            }
            "speech" -> open(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.googlequicksearchbox"))
            )
            "model" -> lifecycleScope.launch {
                VoiceRecognizer.ensureModel(this@SetupActivity)
                renderChecks()
            }
        }
    }

    private fun simulateCall() {
        val until = System.currentTimeMillis() + 40_000L
        var result = "không có câu trả lời"
        App.appScope.launch {
            VoiceCallPrompt(
                context = applicationContext,
                callerLabel = "Người thử nghiệm",
                appName = null,
                isRinging = { System.currentTimeMillis() < until && result == "không có câu trả lời" },
                onAnswer = { result = "nghe" },
                onDecline = { result = "không" },
            ).run()
            Speaker.speak("Kết thúc giả lập. Kết quả: $result.")
        }
    }

    // ---------------- Giao diện nhỏ ----------------

    private fun dp(v: Int) = BigUi.dp(this, v)

    private fun heading(t: String) = TextView(this).apply {
        text = t
        setTextColor(Color.BLACK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(18), 0, dp(6))
    }

    private fun note(t: String) = TextView(this).apply {
        text = t
        setTextColor(0xFF424242.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setPadding(0, dp(4), 0, dp(8))
    }

    private fun button(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    @Suppress("UseSwitchCompatOrMaterialCode")
    private fun toggle(t: String, value: Boolean, onChange: (Boolean) -> Unit): View = Switch(this).apply {
        text = t
        isChecked = value
        setTextColor(Color.BLACK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setPadding(0, dp(8), 0, dp(8))
        setOnCheckedChangeListener { _: CompoundButton, checked: Boolean -> onChange(checked) }
    }

    private fun open(i: Intent): Boolean = try {
        startActivity(i)
        true
    } catch (e: Exception) {
        false
    }

    private fun versionName(): String = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull() ?: "?"

    companion object {
        private val LEVELS = listOf(
            1 to "Mức 1 – bình thường",
            2 to "Mức 2 – to",
            3 to "Mức 3 – rất to (khuyên dùng)",
            4 to "Mức 4 – to nhất",
        )
        private const val GREEN = 0xFF2E7D32.toInt()
        private const val RED = 0xFFC62828.toInt()

        private val TRIGGER_GUIDE = """
            • Mọi máy: GIỮ phím GIẢM âm lượng khoảng 1,5 giây (máy rung nhẹ) → nghe "Mời nói lệnh sau tiếng bíp". Bấm nhanh vẫn giảm âm lượng bình thường. Cần bật Hỗ trợ tiếp cận cho Trợ Lý ở mục 1.
            • Màn hình đang tắt: app vẫn thử nhận phím và tự bật sáng màn hình; nếu máy không rung thì bấm nút nguồn cho sáng rồi giữ lại.
            • Samsung có thêm cách: Cài đặt → Tính năng nâng cao → Phím sườn → Nhấn và giữ → Trợ lý kỹ thuật số → Trợ Lý Nghe Gọi.
        """.trimIndent()

        private val OPPO_GUIDE = """
            Máy Oppo (ColorOS) tắt app chạy nền rất mạnh. Làm đủ các bước cho CẢ Trợ Lý Nghe Gọi, Zalo và WhatsApp:
            • Cài đặt → Quản lý ứng dụng → chọn app → bật "Cho phép tự khởi chạy" và "Cho phép ứng dụng khác khởi chạy".
            • Cùng màn hình đó → Mức sử dụng pin → bật "Cho phép hoạt động nền" (tắt tối ưu hóa pin).
            • Cùng màn hình đó → Quyền ứng dụng / Quyền khác → bật "Hiển thị trên màn hình khóa", "Cửa sổ bật lên" (mở giao diện khi chạy nền), "Hiển thị trên các ứng dụng khác".
            • Mở danh sách app gần đây → bấm ⋮ trên thẻ Trợ Lý (hoặc kéo thẻ xuống) → "Khóa", để không bị dọn khi bấm "Xóa tất cả".
            • Cài đặt → Pin → tắt "Chế độ tiết kiệm pin" và "Tối ưu hóa sạc thông minh/ban đêm" nếu có.
            • Nút nguồn trên Oppo chỉ mở được Camera / Trợ lý Google, nên dùng cách GIỮ phím giảm âm lượng.
            • Không cần mở app thường xuyên. Không bấm "Buộc dừng" app này.
        """.trimIndent()

        private val SAMSUNG_GUIDE = """
            • Pin: Cài đặt → Ứng dụng → Trợ Lý Nghe Gọi → Pin → chọn "Không hạn chế".
            • Không cho ngủ: Cài đặt → Pin → Giới hạn sử dụng nền → Ứng dụng không bao giờ ngủ → thêm Trợ Lý Nghe Gọi, Zalo và WhatsApp.
            • Zalo và WhatsApp cũng đặt Pin = "Không hạn chế" để cuộc gọi đến không bị trễ.
            • Tắt "Tạm dừng hoạt động ứng dụng nếu không dùng" cho Trợ Lý Nghe Gọi (mục ❌ ở trên có nút Sửa).
            • Không cần mở app thường xuyên: vuốt tắt app hay khởi động lại máy đều không ảnh hưởng.
            • Không bấm "Buộc dừng" app này.
        """.trimIndent()
    }
}
