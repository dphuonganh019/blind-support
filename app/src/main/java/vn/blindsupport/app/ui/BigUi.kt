package vn.blindsupport.app.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Giao diện chữ to, nút to, tương phản cao – dễ dùng cho người khiếm thị và người hỗ trợ. */
object BigUi {
    const val GREEN = 0xFF1B5E20.toInt()
    const val RED = 0xFFB71C1C.toInt()
    const val BLUE = 0xFF0D47A1.toInt()
    const val GREY = 0xFF37474F.toInt()
    const val BG = 0xFF000000.toInt()

    fun dp(ctx: Context, v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), ctx.resources.displayMetrics).toInt()

    fun column(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(BG)
        val p = dp(ctx, 12)
        setPadding(p, p, p, p)
    }

    fun title(ctx: Context, size: Float = 30f): TextView = TextView(ctx).apply {
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        val p = dp(ctx, 8)
        setPadding(p, p, p, p)
    }

    fun bigButton(ctx: Context, color: Int): Button = Button(ctx).apply {
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 36f)
        typeface = Typeface.DEFAULT_BOLD
        isAllCaps = false
        background = GradientDrawable().apply {
            cornerRadius = dp(ctx, 24).toFloat()
            setColor(color)
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            val m = dp(ctx, 8)
            setMargins(0, m, 0, m)
        }
    }

    fun setButtonColor(button: Button, color: Int) {
        (button.background as? GradientDrawable)?.setColor(color)
    }

    /**
     * Android 15 vẽ app tràn cả dưới thanh trạng thái và thanh điều hướng. Thêm khoảng đệm đúng
     * bằng các thanh đó để nút bấm không bị thanh điều hướng che.
     */
    fun fitSystemBars(view: android.view.View, lightBars: Boolean = false, activity: Activity? = null) {
        val l = view.paddingLeft
        val t = view.paddingTop
        val r = view.paddingRight
        val b = view.paddingBottom
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                    androidx.core.view.WindowInsetsCompat.Type.displayCutout() or
                    androidx.core.view.WindowInsetsCompat.Type.ime()
            )
            v.setPadding(l + bars.left, t + bars.top, r + bars.right, b + bars.bottom)
            androidx.core.view.WindowInsetsCompat.CONSUMED
        }
        androidx.core.view.ViewCompat.requestApplyInsets(view)
        if (activity != null) {
            androidx.core.view.WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }
    }

    /** Cho màn hình hiện trên màn khóa và tự bật sáng. */
    fun showOverLockScreen(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 27) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        }
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
