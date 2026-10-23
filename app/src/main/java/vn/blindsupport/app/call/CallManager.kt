package vn.blindsupport.app.call

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import vn.blindsupport.app.core.ContactsRepo
import vn.blindsupport.app.core.TextUtils

@Suppress("DEPRECATION")
fun Call.stateCompat(): Int = if (Build.VERSION.SDK_INT >= 31) details.state else state

fun Call.number(): String? = details.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }

fun Call.isHiddenNumber(): Boolean =
    details.handlePresentation != TelecomManager.PRESENTATION_ALLOWED || number() == null

/** Tên hiển thị trên màn hình. */
fun Call.displayName(context: Context): String {
    val n = number()
    val name = (if (Build.VERSION.SDK_INT >= 30) details.contactDisplayName else null)
        ?.takeIf { it.isNotBlank() }
        ?: ContactsRepo.lookupName(context, n)
    return when {
        !name.isNullOrBlank() -> name
        isHiddenNumber() -> "Số ẩn"
        else -> n ?: "Không rõ số"
    }
}

/** Cách đọc to: tên danh bạ, hoặc đọc từng chữ số. */
fun Call.spokenName(context: Context): String {
    val n = number()
    val name = (if (Build.VERSION.SDK_INT >= 30) details.contactDisplayName else null)
        ?.takeIf { it.isNotBlank() }
        ?: ContactsRepo.lookupName(context, n)
    return when {
        !name.isNullOrBlank() -> name
        isHiddenNumber() -> "số ẩn"
        else -> "số " + TextUtils.speakableNumber(n)
    }
}

/** Trạng thái các cuộc gọi sim hiện có, để màn hình cuộc gọi theo dõi. */
object CallManager {
    @Volatile var service: BlindInCallService? = null

    private val _version = MutableStateFlow(0L)
    /** Tăng mỗi khi có thay đổi (cuộc gọi mới, đổi trạng thái, đổi loa...). */
    val version: StateFlow<Long> = _version

    fun notifyChanged() {
        _version.value = _version.value + 1
    }

    fun calls(): List<Call> = service?.calls?.toList() ?: emptyList()

    fun ringingCall(): Call? = calls().firstOrNull { it.stateCompat() == Call.STATE_RINGING }

    /** Cuộc gọi chính để hiển thị: ưu tiên cuộc đang đổ chuông, rồi đang nói chuyện. */
    fun primaryCall(): Call? {
        val list = calls()
        return list.firstOrNull { it.stateCompat() == Call.STATE_RINGING }
            ?: list.firstOrNull { it.stateCompat() == Call.STATE_ACTIVE }
            ?: list.firstOrNull {
                it.stateCompat() in setOf(
                    Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_PULLING_CALL,
                    Call.STATE_SELECT_PHONE_ACCOUNT, Call.STATE_NEW, Call.STATE_HOLDING,
                )
            }
            ?: list.firstOrNull { it.stateCompat() != Call.STATE_DISCONNECTED }
    }

    fun answer(call: Call) {
        runCatching { call.answer(VideoProfile.STATE_AUDIO_ONLY) }
    }

    fun decline(call: Call) {
        runCatching { call.reject(false, null) }
    }

    fun hangup(call: Call) {
        runCatching { call.disconnect() }
    }

    fun isSpeakerOn(): Boolean = service?.isSpeakerOn() == true

    fun setSpeaker(on: Boolean) {
        service?.setSpeaker(on)
    }
}
