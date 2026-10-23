package vn.blindsupport.app.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
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
import kotlinx.coroutines.launch
import vn.blindsupport.app.App
import vn.blindsupport.app.core.AppVolume
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

        col.addView(heading("Cài đặt Trợ Lý Nghe Gọi"))
        col.addView(note("Phiên bản ${versionName()}. Màn hình này dành cho người hỗ trợ. Hãy làm cho tới khi tất cả các mục đều có dấu ✅."))

        col.addView(heading("1. Quyền và trạng thái"))
        checks = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(checks)
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
        col.addView(button("Đặt âm lượng giọng đọc 80%") {
            AppVolume.setPercent(this, 80)
            lifecycleScope.launch { Speaker.speak("Âm lượng giọng đọc tám mươi phần trăm.") }
        })
        col.addView(button("Thử nhận lệnh giọng nói") {
            startActivity(Intent(this, VoiceCommandActivity::class.java))
        })
        col.addView(button("Giả lập cuộc gọi đến (không gọi thật)") { simulateCall() })

        col.addView(heading("3. Tùy chọn"))
        col.addView(toggle("Hỏi \"có muốn nghe không\" và nghe câu trả lời bằng giọng nói", Prefs.voiceAnswer) { Prefs.voiceAnswer = it })
        col.addView(toggle("Tự bật loa ngoài khi nghe máy", Prefs.autoSpeaker) { Prefs.autoSpeaker = it })
        col.addView(toggle("Báo cuộc gọi Zalo / WhatsApp", Prefs.voipCalls) { Prefs.voipCalls = it })
        col.addView(toggle("Đọc tin nhắn Zalo", Prefs.readZalo) { Prefs.readZalo = it })
        col.addView(toggle("Đọc tin nhắn WhatsApp", Prefs.readWhatsapp) { Prefs.readWhatsapp = it })
        col.addView(toggle("Đọc tin nhắn SMS", Prefs.readSms) { Prefs.readSms = it })
        col.addView(toggle("Đọc to cả tin nhắn NHÓM (thường rất nhiều)", Prefs.readGroups) { Prefs.readGroups = it })
        col.addView(toggle("Đọc nội dung tin (tắt: chỉ đọc tên người gửi)", Prefs.readContent) { Prefs.readContent = it })
        col.addView(toggle("Báo pin yếu / cắm sạc / rút sạc", Prefs.announceBattery) { Prefs.announceBattery = it })
        col.addView(toggle(
            "Giờ yên tĩnh ${Prefs.quietStart}h – ${Prefs.quietEnd}h: không đọc tin nhắn (vẫn báo cuộc gọi)",
            Prefs.quietHours
        ) { Prefs.quietHours = it })

        col.addView(heading("4. Cài đặt trên máy Samsung (làm một lần)"))
        col.addView(note(SAMSUNG_GUIDE))
        col.addView(button("Mở cài đặt nút sườn (bấm đúp nút nguồn)") {
            open(Intent(Settings.ACTION_SETTINGS))
        })
    }

    override fun onResume() {
        super.onResume()
        renderChecks()
    }

    private fun renderChecks() {
        checks.removeAllViews()
        lifecycleScope.launch {
            VoiceRecognizer.ensureModel(this@SetupActivity)
            checks.removeAllViews()
            for (item in HealthCheck.run(this@SetupActivity)) {
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
            "a11y" -> open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
            "tts" -> {
                if (!open(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))) {
                    open(Intent("com.android.settings.TTS_SETTINGS"))
                }
            }
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
        private val SAMSUNG_GUIDE = """
            • Bấm đúp nút nguồn để ra lệnh: Cài đặt → Tính năng nâng cao → Phím sườn → Nhấn hai lần → Mở ứng dụng → chọn "Trợ Lý Nghe Gọi".
            • Pin: Cài đặt → Ứng dụng → Trợ Lý Nghe Gọi → Pin → chọn "Không hạn chế".
            • Không cho ngủ: Cài đặt → Pin → Giới hạn sử dụng nền → Ứng dụng không bao giờ ngủ → thêm Trợ Lý Nghe Gọi, Zalo và WhatsApp.
            • Zalo và WhatsApp cũng đặt Pin = "Không hạn chế" để cuộc gọi đến không bị trễ.
            • Tắt "Tạm dừng hoạt động ứng dụng nếu không dùng" cho Trợ Lý Nghe Gọi (mục ❌ ở trên có nút Sửa).
            • Không cần mở app thường xuyên: vuốt tắt app hay khởi động lại máy đều không ảnh hưởng.
            • Không bấm "Buộc dừng" app này.
        """.trimIndent()
    }
}
