package vn.blindsupport.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

/**
 * Bàn phím gọi số đơn giản (dành cho người hỗ trợ). Android yêu cầu ứng dụng điện thoại
 * mặc định phải có màn hình này.
 */
class DialActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = BigUi.column(this)
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "Nhập số điện thoại"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            gravity = Gravity.CENTER
            intent?.data?.schemeSpecificPart?.let { setText(it) }
        }
        val call: Button = BigUi.bigButton(this, BigUi.GREEN).apply {
            text = "GỌI"
            setOnClickListener {
                val number = input.text.toString().trim()
                if (number.isEmpty()) return@setOnClickListener
                if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 1)
                    return@setOnClickListener
                }
                try {
                    getSystemService(TelecomManager::class.java)
                        .placeCall(Uri.fromParts("tel", number, null), Bundle())
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@DialActivity, "Không gọi được: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        val close: Button = BigUi.bigButton(this, BigUi.GREY).apply {
            text = "ĐÓNG"
            setOnClickListener { finish() }
        }
        root.addView(input)
        root.addView(call)
        root.addView(close)
        setContentView(root)
    }
}
