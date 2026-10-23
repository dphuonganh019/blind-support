package vn.blindsupport.app.core

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Giọng đọc tiếng Việt (Text-to-Speech) chạy offline.
 *
 * Mọi câu nói đều phát qua luồng âm thanh "Chuông báo" (alarm), vì luồng này:
 *  - vẫn phát khi máy để im lặng / rung,
 *  - không bị tắt khi app tạm hạ nhỏ nhạc chuông lúc có cuộc gọi.
 *
 * Trước mỗi câu, app đặt lại âm lượng "Chuông báo" theo cài đặt (mặc định 100%), rồi:
 *  1. đọc câu ra file WAV,
 *  2. khuếch đại bằng [AudioBooster] (to hơn mức tối đa của máy),
 *  3. phát bằng AudioTrack.
 * Nếu bước nào lỗi thì đọc trực tiếp như bình thường để không bao giờ im lặng.
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
    @Volatile private var currentTrack: AudioTrack? = null

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
            AppVolume.applyVoiceVolume(appContext)

            val boosted = try {
                speakBoosted(t, text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Khuếch đại lỗi, đọc trực tiếp", e)
                false
            }
            boosted || speakDirect(t, text)
        }
    }

    private suspend fun speakBoosted(t: TextToSpeech, text: String): Boolean {
        val id = UUID.randomUUID().toString()
        val file = File(appContext.cacheDir, "tts_$id.wav")
        try {
            val done = CompletableDeferred<Unit>()
            pending[id] = done
            if (t.synthesizeToFile(text, Bundle(), file, id) != TextToSpeech.SUCCESS) {
                pending.remove(id)
                return false
            }
            val finished = try {
                withTimeoutOrNull(6_000L + text.length * 60L) { done.await() }
            } catch (e: CancellationException) {
                runCatching { t.stop() }
                throw e
            }
            pending.remove(id)
            if (finished == null || !file.exists() || file.length() < 100) return false

            val pcm = withContext(Dispatchers.Default) {
                AudioBooster.readWav(file)?.let { AudioBooster.process(it, Prefs.boostLevel) }
            } ?: return false
            return play(pcm)
        } finally {
            pending.remove(id)
            runCatching { file.delete() }
        }
    }

    private suspend fun play(pcm: Pcm): Boolean {
        if (pcm.frames == 0) return true
        val channelMask = if (pcm.channels == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(pcm.sampleRate)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.samples.size * 2)
            .build()
        try {
            if (track.write(pcm.samples, 0, pcm.samples.size) < 0) return false
            currentTrack = track
            track.play()
            val durationMs = pcm.frames * 1000L / pcm.sampleRate
            val deadline = System.currentTimeMillis() + durationMs + 1_500
            while (System.currentTimeMillis() < deadline) {
                if (currentTrack !== track) break // bị dừng bởi stop()
                if (track.playbackHeadPosition >= pcm.frames) break
                delay(50)
            }
            return true
        } finally {
            if (currentTrack === track) currentTrack = null
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }

    private suspend fun speakDirect(t: TextToSpeech, text: String): Boolean {
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>()
        pending[id] = done
        if (t.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), id) != TextToSpeech.SUCCESS) {
            pending.remove(id)
            return false
        }
        // Ước lượng thời gian đọc tối đa để không bao giờ treo nếu engine không báo xong.
        val maxMs = 4_000L + text.length * 140L
        try {
            withTimeoutOrNull(maxMs) { done.await() }
        } catch (e: CancellationException) {
            runCatching { t.stop() }
            throw e
        } finally {
            pending.remove(id)
        }
        return true
    }

    /** Dừng câu đang đọc (ví dụ khi có cuộc gọi đến cắt ngang việc đọc tin nhắn). */
    fun stop() {
        runCatching { tts?.stop() }
        currentTrack?.let { tr ->
            currentTrack = null
            runCatching { tr.pause(); tr.flush() }
        }
        pending.values.forEach { it.complete(Unit) }
        pending.clear()
    }

    fun isSpeaking(): Boolean =
        currentTrack != null || runCatching { tts?.isSpeaking == true }.getOrDefault(false)

    private fun isInstalled(pkg: String): Boolean = try {
        appContext.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
