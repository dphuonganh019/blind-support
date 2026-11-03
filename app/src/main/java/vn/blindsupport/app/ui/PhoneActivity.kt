package vn.blindsupport.app.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.blindsupport.app.call.CallNotifications
import vn.blindsupport.app.core.Contact
import vn.blindsupport.app.core.ContactsRepo
import vn.blindsupport.app.core.RecentCall
import vn.blindsupport.app.core.TextUtils

/**
 * Ứng dụng điện thoại đầy đủ cho người sáng mắt (khách đến thăm, người hỗ trợ):
 * bàn phím gọi số, nhật ký cuộc gọi gần đây và danh bạ có ô tìm kiếm.
 *
 * Cần có màn hình này vì khi Trợ Lý là ứng dụng điện thoại mặc định, app Điện thoại của Samsung
 * sẽ không cho gọi nữa.
 */
class PhoneActivity : ComponentActivity() {

    private enum class Tab { KEYPAD, RECENT, CONTACTS }

    private lateinit var content: FrameLayout
    private val tabButtons = HashMap<Tab, Button>()
    private var currentTab = Tab.KEYPAD

    private val number = StringBuilder()
    private var numberView: TextView? = null
    private var matchView: TextView? = null
    private var tone: ToneGenerator? = null

    private var contacts: List<Contact> = emptyList()
    private var contactList: LinearLayout? = null

    private val neededPermissions = arrayOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
        }
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(BAR)
            val p = dp(6)
            setPadding(p, p, p, p)
        }
        for ((tab, title) in listOf(Tab.KEYPAD to "Bàn phím", Tab.RECENT to "Gần đây", Tab.CONTACTS to "Danh bạ")) {
            val b = Button(this).apply {
                text = title
                isAllCaps = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setOnClickListener { show(tab) }
            }
            tabButtons[tab] = b
            bar.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                val m = dp(3)
                setMargins(m, 0, m, 0)
            })
        }
        root.addView(bar)
        setContentView(root)
        BigUi.fitSystemBars(root, activity = this)

        tone = runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 70) }.getOrNull()
        handleIntent(intent)

        val missing = neededPermissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 1)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Danh sách có thể đã thay đổi (vừa gọi xong, vừa thêm liên hệ).
        if (currentTab != Tab.KEYPAD) show(currentTab)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        show(currentTab)
    }

    override fun onDestroy() {
        tone?.release()
        tone = null
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        val tel = intent?.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart
        when {
            !tel.isNullOrBlank() -> {
                number.clear()
                number.append(tel.filter { it.isDigit() || it in "+*#" })
                show(Tab.KEYPAD)
            }
            intent?.getStringExtra(EXTRA_TAB) == TAB_RECENT -> show(Tab.RECENT)
            else -> show(currentTab)
        }
    }

    private fun show(tab: Tab) {
        currentTab = tab
        for ((t, b) in tabButtons) {
            b.background = rounded(if (t == tab) ACCENT else BAR_BUTTON, 14)
            b.setTextColor(Color.WHITE)
            b.setTypeface(null, if (t == tab) Typeface.BOLD else Typeface.NORMAL)
        }
        content.removeAllViews()
        numberView = null
        matchView = null
        contactList = null
        when (tab) {
            Tab.KEYPAD -> content.addView(buildKeypad())
            Tab.RECENT -> content.addView(buildRecent())
            Tab.CONTACTS -> content.addView(buildContacts())
        }
        if (tab == Tab.RECENT) CallNotifications.cancelMissed(this)
    }

    // ---------------- Bàn phím ----------------

    private fun buildKeypad(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(12)
            setPadding(p, p, p, p)
        }
        numberView = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 38f)
            gravity = Gravity.CENTER
            setSingleLine()
            setPadding(0, dp(24), 0, dp(4))
        }
        matchView = TextView(this).apply {
            setTextColor(0xFFB0BEC5.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
        }
        // Phần hiển thị số chiếm chỗ trống phía trên, bàn phím gọn ở phía dưới (cỡ bình thường).
        val display = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(numberView)
            addView(matchView)
        }
        col.addView(display, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val keys = listOf(
            listOf("1" to "", "2" to "ABC", "3" to "DEF"),
            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
            listOf("*" to "", "0" to "+", "#" to ""),
        )
        for (row in keys) {
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for ((digit, letters) in row) {
                val b = Button(this).apply {
                    text = if (letters.isEmpty()) digit else android.text.SpannableString("$digit\n$letters").apply {
                        setSpan(
                            android.text.style.RelativeSizeSpan(0.45f), digit.length + 1, length,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                    setPadding(0, 0, 0, 0)
                    minHeight = 0
                    minimumHeight = 0
                    background = rounded(KEY, 30)
                    setOnClickListener { press(digit) }
                    if (digit == "0") setOnLongClickListener { press("+"); true }
                }
                r.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    val m = dp(4)
                    setMargins(m, m, m, m)
                })
            }
            col.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(68)))
        }

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val add = Button(this).apply {
            text = "Lưu số"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            background = rounded(BAR_BUTTON, 40)
            setOnClickListener { addContact(number.toString()) }
        }
        val call = Button(this).apply {
            text = "GỌI"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTypeface(null, Typeface.BOLD)
            background = rounded(GREEN, 40)
            setOnClickListener { placeCall(number.toString()) }
        }
        val del = Button(this).apply {
            text = "⌫"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            background = rounded(BAR_BUTTON, 40)
            setOnClickListener {
                if (number.isNotEmpty()) number.deleteCharAt(number.length - 1)
                renderNumber()
            }
            setOnLongClickListener {
                number.clear()
                renderNumber()
                true
            }
        }
        for ((v, w) in listOf(add to 1f, call to 1.4f, del to 1f)) {
            actions.addView(v, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, w).apply {
                val m = dp(5)
                setMargins(m, m, m, m)
            })
        }
        for (v in listOf(add, call, del)) {
            v.minHeight = 0
            v.minimumHeight = 0
        }
        col.addView(actions, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)))
        renderNumber()
        return col
    }

    private fun press(key: String) {
        if (number.length >= 20) return
        number.append(key)
        val toneId = when (key) {
            "*" -> ToneGenerator.TONE_DTMF_S
            "#" -> ToneGenerator.TONE_DTMF_P
            "+" -> -1
            else -> key.toIntOrNull()?.let { ToneGenerator.TONE_DTMF_0 + it } ?: -1
        }
        if (toneId >= 0) runCatching { tone?.startTone(toneId, 120) }
        renderNumber()
    }

    private fun renderNumber() {
        val n = number.toString()
        numberView?.text = n.ifEmpty { " " }
        if (n.length < 3) {
            matchView?.text = " "
            return
        }
        lifecycleScope.launch {
            val name = withContext(Dispatchers.IO) { ContactsRepo.lookupName(this@PhoneActivity, n) }
            if (number.toString() == n) matchView?.text = name ?: " "
        }
    }

    // ---------------- Gần đây ----------------

    private fun buildRecent(): View {
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(8)
            setPadding(p, p, p, p)
        }
        scroll.addView(list)
        list.addView(hint("Đang tải..."))
        lifecycleScope.launch {
            val calls = withContext(Dispatchers.IO) { ContactsRepo.recentCalls(this@PhoneActivity, 100) }
            list.removeAllViews()
            if (calls.isEmpty()) {
                list.addView(hint(
                    if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
                        "Chưa có quyền xem nhật ký cuộc gọi." else "Chưa có cuộc gọi nào."
                ))
                return@launch
            }
            for (c in calls) list.addView(recentRow(c))
        }
        return scroll
    }

    private fun recentRow(c: RecentCall): View {
        val (label, color) = when (c.type) {
            CallLog.Calls.MISSED_TYPE -> "Gọi nhỡ" to 0xFFEF5350.toInt()
            CallLog.Calls.OUTGOING_TYPE -> "Gọi đi" to 0xFF81C784.toInt()
            CallLog.Calls.REJECTED_TYPE -> "Đã từ chối" to 0xFFFFB74D.toInt()
            else -> "Gọi đến" to 0xFF64B5F6.toInt()
        }
        val title = c.name ?: c.number ?: "Số ẩn"
        val sub = buildString {
            append(label)
            append(" · ")
            append(whenText(c.time))
            if (c.name != null && c.number != null) append(" · ").append(c.number)
        }
        return row(title, sub, color, c.number)
    }

    private fun whenText(millis: Long): String {
        val t = TextUtils.shortWhen(millis)
        return t.removePrefix("lúc ").replace(" giờ ", ":").let { s ->
            // "9:5" → "9:05"
            Regex("(\\d+):(\\d+)$").find(s)?.let { m ->
                s.substring(0, m.range.first) + m.groupValues[1] + ":" + m.groupValues[2].padStart(2, '0')
            } ?: s
        }
    }

    // ---------------- Danh bạ ----------------

    private fun buildContacts(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = dp(8)
            setPadding(p, p, p, p)
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val search = EditText(this).apply {
            hint = "Tìm tên hoặc số"
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF90A4AE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setSingleLine()
            background = rounded(KEY, 14)
            val p = dp(12)
            setPadding(p, p, p, p)
        }
        val add = Button(this).apply {
            text = "+ Thêm"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            background = rounded(ACCENT, 14)
            setOnClickListener { addContact(null) }
        }
        top.addView(search, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(add, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            setMargins(dp(8), 0, 0, 0)
        })
        col.addView(top)

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        col.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            setMargins(0, dp(8), 0, 0)
        })
        contactList = list
        list.addView(hint("Đang tải..."))

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { renderContacts(s?.toString() ?: "") }
        })
        lifecycleScope.launch {
            contacts = withContext(Dispatchers.IO) { ContactsRepo.allPhoneEntries(this@PhoneActivity) }
            renderContacts(search.text.toString())
        }
        return col
    }

    private fun renderContacts(query: String) {
        val list = contactList ?: return
        list.removeAllViews()
        if (contacts.isEmpty()) {
            list.addView(hint(
                if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
                    "Chưa có quyền xem danh bạ." else "Danh bạ trống."
            ))
            return
        }
        val q = TextUtils.fold(query)
        val digits = query.filter { it.isDigit() }
        val filtered = if (q.isBlank()) contacts else contacts.filter {
            TextUtils.fold(it.name).contains(q) ||
                (digits.length >= 2 && it.number.filter { ch -> ch.isDigit() }.contains(digits))
        }
        if (filtered.isEmpty()) {
            list.addView(hint("Không tìm thấy."))
            return
        }
        for (c in filtered.take(300)) list.addView(row(c.name, c.number, null, c.number))
        if (filtered.size > 300) list.addView(hint("Còn ${filtered.size - 300} người nữa, hãy gõ để tìm."))
    }

    // ---------------- Dùng chung ----------------

    /** Một dòng: chạm vào dòng = đưa số lên bàn phím; nút GỌI bên phải = gọi luôn. */
    private fun row(title: String, sub: String, subColor: Int?, number: String?): View {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(ROW, 14)
            val p = dp(12)
            setPadding(p, p, p, p)
        }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            setSingleLine()
        })
        texts.addView(TextView(this).apply {
            text = sub
            setTextColor(subColor ?: 0xFFB0BEC5.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setSingleLine()
        })
        r.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (number != null) {
            r.setOnClickListener {
                this.number.clear()
                this.number.append(number.filter { it.isDigit() || it in "+*#" })
                show(Tab.KEYPAD)
            }
            r.addView(Button(this).apply {
                text = "GỌI"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(null, Typeface.BOLD)
                background = rounded(GREEN, 30)
                setOnClickListener { placeCall(number) }
            }, LinearLayout.LayoutParams(dp(84), dp(52)).apply { setMargins(dp(8), 0, 0, 0) })
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(4))
            addView(r)
        }
    }

    private fun hint(t: String) = TextView(this).apply {
        text = t
        setTextColor(0xFFB0BEC5.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        gravity = Gravity.CENTER
        setPadding(0, dp(32), 0, dp(32))
    }

    private fun placeCall(raw: String) {
        val n = raw.trim()
        if (n.isEmpty()) {
            Toast.makeText(this, "Hãy nhập số điện thoại", Toast.LENGTH_SHORT).show()
            return
        }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 2)
            return
        }
        try {
            getSystemService(TelecomManager::class.java).placeCall(Uri.fromParts("tel", n, null), Bundle())
            number.clear()
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setMessage("Không gọi được: ${e.message}")
                .setPositiveButton("Đóng", null)
                .show()
        }
    }

    private fun addContact(phone: String?) {
        val i = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
        if (!phone.isNullOrBlank()) i.putExtra(ContactsContract.Intents.Insert.PHONE, phone)
        try {
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, "Không mở được ứng dụng Danh bạ", Toast.LENGTH_LONG).show()
        }
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color)
    }

    private fun dp(v: Int) = BigUi.dp(this, v)

    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_RECENT = "recent"

        private const val BG = 0xFF101418.toInt()
        private const val BAR = 0xFF1A2026.toInt()
        private const val BAR_BUTTON = 0xFF2A323A.toInt()
        private const val KEY = 0xFF232B33.toInt()
        private const val ROW = 0xFF1C232A.toInt()
        private const val ACCENT = 0xFF1565C0.toInt()
        private const val GREEN = 0xFF2E7D32.toInt()
    }
}
