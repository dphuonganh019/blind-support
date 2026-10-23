package vn.blindsupport.app.core

import android.content.Context
import kotlinx.coroutines.delay

enum class CallIntent { ANSWER, DECLINE;

    companion object {
        val ANSWER_WORDS = listOf("nghe", "nghe máy", "nghe đi", "có", "vâng", "ừ", "bắt máy", "trả lời", "đồng ý", "alo", "ok")
        val DECLINE_WORDS = listOf("không", "không nghe", "từ chối", "thôi", "bận", "tắt", "tắt máy", "cúp máy")
        val PHRASES = ANSWER_WORDS + DECLINE_WORDS

        fun parse(text: String): CallIntent? {
            val t = " " + TextUtils.normalize(text) + " "
            if (t.isBlank()) return null
            // Kiểm tra từ chối trước: "không nghe" có chứa chữ "nghe".
            if (DECLINE_WORDS.any { t.contains(" $it ") }) return DECLINE
            if (ANSWER_WORDS.any { t.contains(" $it ") }) return ANSWER
            return null
        }
    }
}

/**
 * Hỏi người dùng có muốn nghe cuộc gọi không và nghe câu trả lời bằng giọng nói.
 * Dùng chung cho cuộc gọi sim, Zalo và WhatsApp.
 *
 * Vòng lặp: (hạ chuông) đọc tên người gọi → bíp → nghe 5 giây → (trả chuông) đổ chuông vài giây → lặp lại,
 * cho tới khi người dùng trả lời, bấm nút, hoặc người gọi cúp máy.
 */
class VoiceCallPrompt(
    private val context: Context,
    private val callerLabel: String,
    /** null với cuộc gọi sim, "Zalo"/"WhatsApp" với cuộc gọi qua mạng. */
    private val appName: String?,
    private val isRinging: () -> Boolean,
    private val onAnswer: suspend () -> Unit,
    private val onDecline: suspend () -> Unit,
) {
    suspend fun run() {
        // Máy đang để im lặng / rung: tôn trọng lựa chọn đó, không nói to.
        if (!AudioState.isRingerNormal(context)) return
        AppVolume.ensureAtLeast(context, 50)
        val kind = if (appName != null) "cuộc gọi $appName" else "cuộc gọi"
        try {
            var round = 0
            while (isRinging() && round < 10) {
                RingVolumeGuard.lower(context)
                val sentence = when {
                    !Prefs.voiceAnswer -> "Có $kind từ $callerLabel."
                    round == 0 -> "Có $kind từ $callerLabel. Bạn có muốn nghe không? Hãy nói: nghe, hoặc: không."
                    else -> "Vẫn đang có $kind từ $callerLabel. Nói: nghe, hoặc: không."
                }
                Speaker.speak(sentence)
                if (!isRinging()) return

                if (Prefs.voiceAnswer) {
                    Beeper.beep()
                    val heard = VoiceRecognizer.listen(context, CallIntent.PHRASES, 5_000) {
                        CallIntent.parse(it) != null
                    }
                    if (!isRinging()) return
                    when (heard?.let { CallIntent.parse(it) }) {
                        CallIntent.ANSWER -> {
                            Speaker.speak("Nghe máy.")
                            onAnswer()
                            return
                        }
                        CallIntent.DECLINE -> {
                            onDecline()
                            Speaker.speak("Đã từ chối cuộc gọi.")
                            return
                        }
                        null -> Unit
                    }
                }

                // Trả lại chuông vài giây để người dùng vẫn biết máy đang đổ chuông.
                RingVolumeGuard.restore(context)
                var waited = 0
                while (waited < 4_000 && isRinging()) {
                    delay(250)
                    waited += 250
                }
                round++
            }
        } finally {
            RingVolumeGuard.restore(context)
        }
    }
}
