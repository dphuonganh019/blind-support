package vn.blindsupport.app.core

import android.content.Context
import android.media.AudioAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.delay

enum class CallIntent { ANSWER, DECLINE;

    companion object {
        val ANSWER_WORDS = listOf("nghe", "nghe máy", "nghe đi", "có", "vâng", "ừ", "bắt máy", "trả lời", "đồng ý")
        /**
         * Chỉ những cụm rõ ràng. Không dùng các từ một âm tiết dễ bị tiếng ồn "đoán" ra như
         * "tắt", "bận", "thôi": từ chối nhầm là mất cuộc gọi.
         */
        val DECLINE_WORDS = listOf("không", "không nghe", "từ chối", "tắt máy", "cúp máy")

        /**
         * Từ "mồi" thường gặp. Đưa vào danh sách nhận dạng để tiếng ồn, tiếng TV, tiếng người
         * nói chuyện xung quanh bị nhận thành các từ này thay vì thành "nghe"/"không".
         */
        val FILLER_WORDS = listOf(
            "à", "ạ", "ơi", "ờ", "ừm", "hả", "nhé", "nha", "đâu", "gì", "nào", "này", "kia", "đó",
            "ai", "ăn", "đi", "về", "làm", "người", "con", "mẹ", "bố", "anh", "chị", "em", "thầy", "cô",
            "sư", "chùa", "phật", "nam", "mô", "a di đà", "chào", "cảm ơn", "xin", "được", "rồi", "chưa",
            "đang", "sẽ", "mà", "thì", "là", "và", "với", "cho", "của", "nhà", "đây", "ở", "ra", "vào",
            "lên", "xuống", "biết", "thấy", "nói", "hỏi", "gọi", "điện", "thoại", "tiếng", "bíp", "chuông",
        )
        val PHRASES = ANSWER_WORDS + DECLINE_WORDS + FILLER_WORDS

        /**
         * Khi chuông KHÔNG tắt được (cuộc gọi Zalo / WhatsApp), tiếng chuông lọt vào micro hay bị
         * nhận thành "có", "ừ", "vâng". Khi đó chỉ nhận những cụm có chữ "nghe" hoặc nói rõ ý.
         */
        val STRICT_ANSWER_WORDS = listOf("nghe", "nghe máy", "nghe đi", "bắt máy", "trả lời")

        private val ANSWER_SET = ANSWER_WORDS.flatMap { TextUtils.words(it) }.toSet()
        private val STRICT_ANSWER_SET = STRICT_ANSWER_WORDS.flatMap { TextUtils.words(it) }.toSet()
        private val DECLINE_SET = DECLINE_WORDS.flatMap { TextUtils.words(it) }.toSet()

        /**
         * Chỉ nhận khi câu nghe được ngắn (tối đa 3 từ) và MỌI từ đều thuộc cùng một nhóm.
         * Câu lẫn từ khác (thường là tiếng ồn hoặc người xung quanh nói chuyện) bị bỏ qua.
         */
        fun parse(text: String, ringerAudible: Boolean = false): CallIntent? {
            val words = TextUtils.words(text)
            if (words.isEmpty() || words.size > 3) return null
            val t = " " + words.joinToString(" ") + " "
            // "không nghe" có chữ "nghe": xét từ chối trước.
            if (words.all { it in DECLINE_SET || it == "nghe" } && DECLINE_WORDS.any { t.contains(" $it ") }) {
                return DECLINE
            }
            if (ringerAudible) {
                if (words.all { it in STRICT_ANSWER_SET } && STRICT_ANSWER_WORDS.any { t.contains(" $it ") }) return ANSWER
                return null
            }
            if (words.all { it in ANSWER_SET } && ANSWER_WORDS.any { t.contains(" $it ") }) return ANSWER
            return null
        }
    }
}

/**
 * Hỏi người dùng có muốn nghe cuộc gọi không và nghe câu trả lời bằng giọng nói.
 * Dùng chung cho cuộc gọi sim, Zalo và WhatsApp.
 *
 * Vòng lặp: đọc tên người gọi → bíp dài → nghe 7 giây → lặp lại, cho tới khi người dùng trả lời,
 * bấm nút, hoặc người gọi cúp máy.
 */
class VoiceCallPrompt(
    private val context: Context,
    private val callerLabel: String,
    /** null với cuộc gọi sim, "Zalo"/"WhatsApp" với cuộc gọi qua mạng. */
    private val appName: String?,
    private val isRinging: () -> Boolean,
    private val onAnswer: suspend () -> Unit,
    private val onDecline: suspend () -> Unit,
    /**
     * Tắt hẳn chuông của cuộc gọi (chỉ làm được với cuộc gọi sim). Khi đã tắt được, tiếng chuông
     * không còn lọt vào micro gây nhận nhầm, và app tự đọc tên người gọi thay cho chuông.
     */
    private val silenceRinger: (() -> Boolean)? = null,
) {
    suspend fun run() {
        // Kể cả khi máy để im lặng / rung vẫn đọc tên người gọi: người khiếm thị có thể đã
        // vô tình gạt sang im lặng mà không biết. Giọng đọc đi qua luồng "Chuông báo" nên vẫn kêu.
        val kind = if (appName != null) "cuộc gọi $appName" else "cuộc gọi"
        var ringerSilenced = false
        // Giữ micro bằng dịch vụ tiền cảnh trong lúc hỏi, để nghe được câu trả lời kể cả khi
        // ColorOS không cho màn hình cuộc gọi bật lên. Không bật được thì vẫn chạy như cũ.
        val micHeld = Prefs.voiceAnswer && MicForegroundService.start(context)
        try {
            // Rung liên tục suốt lúc có cuộc gọi (phòng khi để máy trong túi, nghe kém).
            if (Prefs.vibrateOnCall) startVibration()
            // Để chuông thật của máy reo vài giây trước, cho người nghe kém kịp nhận ra có cuộc gọi.
            // Zalo/WhatsApp đổ chuông ngắn hơn cuộc gọi sim: chỉ chờ khoảng 3 giây (app đã chờ sẵn
            // khoảng 1 giây trước khi bắt đầu hỏi).
            val ringFirstMs = if (appName != null) 2_000L else Prefs.ringFirstSeconds * 1000L
            var rang = 0L
            while (rang < ringFirstMs && isRinging()) {
                delay(250)
                rang += 250
            }
            if (!isRinging()) return
            if (Prefs.voiceAnswer) {
                ringerSilenced = silenceRinger?.invoke() == true
            }
            var round = 0
            while (isRinging() && round < 12) {
                if (!ringerSilenced) RingVolumeGuard.lower(context)
                val attention = if (appName != null) "Có điện thoại $appName! " else "Có điện thoại! "
                val sentence = when {
                    !Prefs.voiceAnswer -> "${attention}Cuộc gọi từ $callerLabel."
                    round == 0 -> "${attention}Cuộc gọi từ $callerLabel. Bạn có muốn nghe không? " +
                        "Hãy nói: nghe, hoặc: không, sau tiếng bíp. Hoặc bấm phím bất kỳ ở cạnh máy để nghe."
                    else -> "${attention}Vẫn đang có $kind từ $callerLabel. Hãy nói: nghe, hoặc: không, sau tiếng bíp."
                }
                Speaker.speak(sentence)
                if (!isRinging()) return
                if (!Prefs.voiceAnswer) return

                Beeper.beep()
                if (!isRinging()) return
                val audible = !ringerSilenced
                val heard = VoiceRecognizer.listen(context, CallIntent.PHRASES, 7_000) {
                    CallIntent.parse(it, audible) != null
                }
                if (!isRinging()) return
                when (heard?.let { CallIntent.parse(it, audible) }) {
                    CallIntent.ANSWER -> {
                        Speaker.speak("Nghe máy.")
                        if (isRinging()) onAnswer()
                        return
                    }
                    CallIntent.DECLINE -> {
                        if (isRinging()) onDecline()
                        Speaker.speak("Đã từ chối cuộc gọi.")
                        return
                    }
                    null -> Unit
                }

                if (!ringerSilenced) {
                    // Trả lại chuông vài giây để người dùng vẫn biết máy đang đổ chuông.
                    RingVolumeGuard.restore(context)
                    var waited = 0
                    while (waited < 3_000 && isRinging()) {
                        delay(250)
                        waited += 250
                    }
                } else {
                    // Chuông thật đã tắt: app tự phát tiếng chuông xen giữa các lần hỏi.
                    if (isRinging()) Speaker.ring(3_000)
                }
                round++
            }
        } finally {
            stopVibration()
            RingVolumeGuard.restore(context)
            if (micHeld) MicForegroundService.stop(context)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrator(): Vibrator? = context.getSystemService(Vibrator::class.java)

    @Suppress("DEPRECATION")
    private fun startVibration() {
        runCatching {
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            vibrator()?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 900, 700), 0), attrs)
        }
    }

    private fun stopVibration() {
        runCatching { vibrator()?.cancel() }
    }
}
