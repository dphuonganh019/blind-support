package vn.blindsupport.app.core

import android.content.Context
import android.media.AudioAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
         * Độ tin cậy tối thiểu để tính là TỪ CHỐI. Cao hơn hẳn "nghe": từ chối nhầm là mất cuộc gọi,
         * còn tiếng ồn, tiếng thở, dư âm tiếng bíp hay bị bộ nhận dạng offline "đoán" thành "không".
         */
        const val DECLINE_MIN_CONF = 0.8

        /** Câu trả lời cho "Từ chối cuộc gọi, đúng không?". */
        private val CONFIRM_DECLINE = listOf("đúng", "đúng rồi", "phải", "vâng", "ừ", "từ chối", "không", "tắt máy")
        val CONFIRM_PHRASES = (CONFIRM_DECLINE + ANSWER_WORDS + FILLER_WORDS).distinct()
        private val CONFIRM_SET = CONFIRM_DECLINE.flatMap { TextUtils.words(it) }.toSet()

        /** ANSWER nếu đổi ý muốn nghe, DECLINE nếu xác nhận từ chối, null nếu không rõ. */
        fun parseDeclineConfirm(text: String): CallIntent? {
            val words = TextUtils.words(text)
            if (words.isEmpty()) return null
            if (hasFreeNghe(words)) return ANSWER
            if (words.size > 3) return null
            // "không nghe" (chữ "nghe" bị phủ định) cũng là xác nhận từ chối.
            return if (words.all { it in CONFIRM_SET || it == "nghe" }) DECLINE else null
        }

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
        /** Chữ đứng ngay trước "nghe" làm nó thành ý từ chối: "không nghe", "chưa nghe", "đừng nghe". */
        private val NEGATORS = setOf("không", "chưa", "đừng")

        /** Câu có chữ "nghe" đứng tự do (không bị "không/chưa/đừng" phủ định ngay phía trước). */
        fun hasFreeNghe(words: List<String>): Boolean =
            words.indices.any { i -> words[i] == "nghe" && (i == 0 || words[i - 1] !in NEGATORS) }

        fun parse(text: String, ringerAudible: Boolean = false): CallIntent? {
            val words = TextUtils.words(text)
            if (words.isEmpty()) return null
            // Có chữ "nghe" ở bất kỳ đâu là nhấc máy, dù trước/sau có chữ lạ ("nghe em", "sẽ nghe"):
            // bộ nhận dạng offline hay ghép thêm chữ từ hơi thở, tiếng ồn. Lỡ nhấc máy ít hại hơn mất cuộc gọi.
            if (hasFreeNghe(words)) return ANSWER
            if (words.size > 3) return null
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
        var ringerSilenced = false
        // Giữ micro bằng dịch vụ tiền cảnh trong lúc hỏi, để nghe được câu trả lời kể cả khi
        // ColorOS không cho màn hình cuộc gọi bật lên. Không bật được thì vẫn chạy như cũ.
        val micHeld = Prefs.voiceAnswer && MicForegroundService.start(context)
        // Chuông máy lên 100% suốt lúc có cuộc gọi (lỡ bị kéo nhỏ cũng vẫn nghe thấy), xong trả lại.
        RingVolumeGuard.raiseForCall(context)
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
                // Câu thật ngắn: nhà mạng chỉ cho đổ chuông khoảng 30–40 giây, phải kịp hỏi 2 lần.
                val attention = if (appName != null) "Có điện thoại $appName! " else "Có điện thoại! "
                // Phím cạnh máy chỉ nghe được cuộc gọi sim (với Zalo/WhatsApp, Android không chuyển phím cho app).
                val sentence = when {
                    !Prefs.voiceAnswer -> "$attention$callerLabel gọi."
                    round == 0 && appName == null -> "$attention$callerLabel gọi. Sau tiếng bíp, nói: nghe. Hoặc bấm phím cạnh máy để nghe."
                    round == 0 -> "$attention$callerLabel gọi. Sau tiếng bíp, nói: nghe, để nghe."
                    else -> "Bạn có muốn nghe máy không? Sau tiếng bíp, nói: nghe."
                }
                Speaker.speak(sentence)
                if (!isRinging()) return
                if (!Prefs.voiceAnswer) return

                callBeep()
                if (!isRinging()) return
                val audible = !ringerSilenced
                // Lần nghe đầu dài hơn: có thể không còn kịp lần thứ hai.
                val heard = VoiceRecognizer.listen(context, CallIntent.PHRASES, if (round == 0) 9_000 else 7_000) {
                    CallIntent.parse(it, audible) != null
                }
                if (!isRinging()) return
                var intent = heard?.let { CallIntent.parse(it, audible) }
                // "Không" nghe không chắc chắn: coi như chưa nghe gì (thường là tiếng ồn).
                if (intent == CallIntent.DECLINE && VoiceRecognizer.lastConfidence < CallIntent.DECLINE_MIN_CONF) {
                    intent = null
                }
                // Hỏi lại trước khi từ chối; im lặng thì KHÔNG từ chối, tiếp tục đổ chuông.
                if (intent == CallIntent.DECLINE) {
                    Speaker.speak("Từ chối cuộc gọi, đúng không?")
                    if (!isRinging()) return
                    callBeep()
                    if (!isRinging()) return
                    val again = VoiceRecognizer.listen(context, CallIntent.CONFIRM_PHRASES, 5_000) {
                        CallIntent.parseDeclineConfirm(it) != null
                    }
                    if (!isRinging()) return
                    intent = again?.let { CallIntent.parseDeclineConfirm(it) }
                }
                when (intent) {
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
                    // Trả lại chuông một chút để người dùng vẫn biết máy đang đổ chuông.
                    RingVolumeGuard.restore(context)
                    var waited = 0
                    while (waited < 1_500 && isRinging()) {
                        delay(250)
                        waited += 250
                    }
                } else {
                    // Chuông thật đã tắt: app tự phát tiếng chuông ngắn xen giữa các lần hỏi.
                    if (isRinging()) Speaker.ring(1_500)
                }
                round++
            }
        } finally {
            stopVibration()
            RingVolumeGuard.restore(context)
            RingVolumeGuard.endCall(context)
            if (micHeld) MicForegroundService.stop(context)
        }
    }

    /**
     * Tiếng bíp ngắn (0,8 giây) cho câu hỏi khi có cuộc gọi, rồi chờ dư âm tắt hẳn mới mở micro
     * (dư âm tiếng bíp hay bị nghe nhầm thành "không").
     */
    private suspend fun callBeep() {
        coroutineScope {
            // Chuẩn bị sẵn bộ nhận dạng trong lúc đang bíp, hết bíp là nghe được ngay.
            val prep = async(Dispatchers.Default) { VoiceRecognizer.prepare(CallIntent.PHRASES) }
            if (!Speaker.longBeep(800)) Beeper.beep()
            prep.await()
        }
        delay(250)
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
