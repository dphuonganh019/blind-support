package vn.blindsupport.app.core

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Giọng đọc tiếng Việt (Text-to-Speech) chạy offline.
 *
 * Mọi câu nói đều phát qua luồng âm thanh "báo thức" (alarm) để:
 *  - không bị tắt tiếng khi app tạm hạ nhỏ chuông lúc có cuộc gọi,
 *  - chỉ có một mức âm lượng cần quản lý (người dùng nói "to hơn"/"nhỏ hơn" để chỉnh).
 */
object Speaker {
    private const val TAG = "Speaker"
    private const val GOOGLE_TTS = "com.google.android.tts"
    private val VI = Locale("vi", "VN")

    private lateinit var appContext: Context
    private var tts: TextToSpeech? = null
    private var ready = CompletableDeferred<Boolean>()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val mutex = Mutex()

    /** true nếu engine hiện tại có giọng tiếng Việt dùng được. */
    @Volatile var vietnameseAvailable = false
        private set
    @Volatile var engineName: String? = null
        private set

    private val attributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    fun init(context: Context) {
        if (tts != null) return
        appContext = context.applicationContext
        val preferGoogle = isInstalled(GOOGLE_TTS)
        create(if (preferGoogle) GOOGLE_TTS else null, allowFallback = preferGoogle)
    }

    private fun create(engine: String?, allowFallback: Boolean) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(appContext, initListener@{ status ->
            val t = instance
            var ok = false
            if (status == TextToSpeech.SUCCESS && t != null) {
                val r = t.setLanguage(VI)
                ok = r >= TextToSpeech.LANG_AVAILABLE
                t.setSpeechRate(Prefs.speechRate)
                t.setAudioAttributes(attributes)
                t.setOnUtteranceProgressListener(progressListener)
            }
            if (!ok && allowFallback) {
                // Giọng Google chưa có tiếng Việt: thử engine mặc định của máy (Samsung).
                Log.w(TAG, "Engine $engine thiếu tiếng Việt, thử engine mặc định")
                runCatching { t?.shutdown() }
                create(null, allowFallback = false)
                return@initListener
            }
            vietnameseAvailable = ok
            engineName = t?.defaultEngine
            tts = t
            ready.complete(status == TextToSpeech.SUCCESS)
        }, engine)
        if (tts == null) tts = instance
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) { finish(utteranceId) }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) { finish(utteranceId) }
        override fun onError(utteranceId: String?, errorCode: Int) { finish(utteranceId) }
        override fun onStop(utteranceId: String?, interrupted: Boolean) { finish(utteranceId) }
    }

    private fun finish(id: String?) {
        if (id != null) pending.remove(id)?.complete(Unit)
    }

    /**
     * Đọc to một câu và chờ đọc xong. Trả về false nếu không đọc được.
     * Nếu coroutine bị hủy (ví dụ người gọi đã cúp máy) thì dừng đọc ngay.
     */
    suspend fun speak(text: String): Boolean {
        if (text.isBlank()) return true
        return mutex.withLock {
            val ok = withTimeoutOrNull(8_000) { ready.await() } ?: false
            val t = tts
            if (!ok || t == null) return@withLock false
            t.setSpeechRate(Prefs.speechRate)
            val id = UUID.randomUUID().toString()
            val done = CompletableDeferred<Unit>()
            pending[id] = done
            val params = Bundle()
            val res = t.speak(text, TextToSpeech.QUEUE_FLUSH, params, id)
            if (res != TextToSpeech.SUCCESS) {
                pending.remove(id)
                return@withLock false
            }
            // Ước lượng thời gian đọc tối đa để không bao giờ treo nếu engine không báo xong.
            val maxMs = 4_000L + text.length * 120L
            try {
                withTimeoutOrNull(maxMs) { done.await() }
            } catch (e: CancellationException) {
                runCatching { t.stop() }
                throw e
            } finally {
                pending.remove(id)
            }
            true
        }
    }

    /** Dừng câu đang đọc (ví dụ khi có cuộc gọi đến cắt ngang việc đọc tin nhắn). */
    fun stop() {
        runCatching { tts?.stop() }
        pending.values.forEach { it.complete(Unit) }
        pending.clear()
    }

    fun isSpeaking(): Boolean = runCatching { tts?.isSpeaking == true }.getOrDefault(false)

    private fun isInstalled(pkg: String): Boolean = try {
        appContext.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
