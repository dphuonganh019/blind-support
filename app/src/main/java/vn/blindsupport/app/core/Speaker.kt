package vn.blindsupport.app.core

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
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
    /**
     * true nếu giọng tiếng Việt đang dùng đã tải về máy (đọc được khi mất mạng).
     * Chỉ kiểm tra "đặt được ngôn ngữ" là không đủ: giọng Google có thể chỉ có bản cần mạng.
     */
    @Volatile var vietnameseOffline = false
        private set
    private val failed: MutableSet<String> = ConcurrentHashMap.newKeySet()
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
            vietnameseOffline = ok && t != null && pickOfflineVoice(t)
            engineName = t?.defaultEngine
            tts = t
            ready.complete(status == TextToSpeech.SUCCESS)
        }, engine)
        if (tts == null) tts = instance
    }

    /** Kiểm tra lại giọng đọc (ví dụ sau khi người hỗ trợ vừa tải giọng tiếng Việt về máy). */
    fun refreshOfflineVoice() {
        val t = tts ?: return
        if (vietnameseAvailable) vietnameseOffline = pickOfflineVoice(t)
    }

    private fun isOffline(v: Voice): Boolean =
        !v.isNetworkConnectionRequired &&
            TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in (v.features ?: emptySet<String>())

    /** Chọn một giọng tiếng Việt đã có trên máy. Trả về false nếu không có giọng nào như vậy. */
    private fun pickOfflineVoice(t: TextToSpeech): Boolean = runCatching {
        val current = t.voice
        if (current != null && current.locale.language == "vi" && isOffline(current)) return@runCatching true
        val best = t.voices.orEmpty()
            .filter { it.locale.language == "vi" && isOffline(it) }
            .maxByOrNull { it.quality }
            ?: return@runCatching false
        t.setVoice(best) == TextToSpeech.SUCCESS
    }.getOrDefault(false)

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) { finish(utteranceId) }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) { fail(utteranceId) }
        override fun onError(utteranceId: String?, errorCode: Int) { fail(utteranceId) }
        override fun onStop(utteranceId: String?, interrupted: Boolean) { finish(utteranceId) }
    }

    private fun fail(id: String?) {
        if (id != null) failed.add(id)
        finish(id)
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
            if (!::appContext.isInitialized) return@withLock false
            val ok = withTimeoutOrNull(8_000) { ready.await() } ?: false
            val t = tts
            withVoiceVolume {
                if (!ok || t == null) {
                    errorTone()
                    return@withVoiceVolume false
                }
                t.setSpeechRate(Prefs.speechRate)
                val boosted = try {
                    speakBoosted(t, text)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Khuếch đại lỗi, đọc trực tiếp", e)
                    false
                }
                val spoken = boosted || speakDirect(t, text)
                // Không đọc được (thường do giọng đọc cần mạng mà máy đang mất mạng): phát tiếng
                // bíp báo lỗi để người dùng biết, thay vì im lặng hoàn toàn.
                if (!spoken) errorTone()
                spoken
            }
        }
    }

    /**
     * Đặt âm lượng "Chuông báo" theo cài đặt của app trong lúc đọc, xong thì trả lại mức cũ,
     * để báo thức đồng hồ không bị kéo lên tối đa.
     */
    private suspend fun <T> withVoiceVolume(block: suspend () -> T): T {
        val am = appContext.getSystemService(AudioManager::class.java)
        val before = am?.let { runCatching { it.getStreamVolume(AudioManager.STREAM_ALARM) }.getOrNull() }
        AppVolume.applyVoiceVolume(appContext)
        try {
            return block()
        } finally {
            if (am != null && before != null) {
                runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, before, 0) }
            }
        }
    }

    /** Ba tiếng "tít" ngắn, cao: báo giọng đọc bị lỗi. */
    private suspend fun errorTone() {
        val sr = 22050
        val pip = sr / 8 // 125 ms
        val gap = sr / 10
        val total = pip * 3 + gap * 2
        val samples = ShortArray(total) { i ->
            val k = i % (pip + gap)
            if (k >= pip) 0.toShort() else {
                val t = k.toDouble() / sr
                val env = when {
                    k < 100 -> k / 100.0
                    k > pip - 100 -> (pip - k) / 100.0
                    else -> 1.0
                }
                (kotlin.math.sin(2 * Math.PI * 1320.0 * t) * env * 0.9 * 32767).toInt().toShort()
            }
        }
        runCatching { play(Pcm(samples, sr, 1)) }
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
            failed.remove(id)
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
        var timedOut = false
        // Ước lượng thời gian đọc tối đa để không bao giờ treo nếu engine không báo xong.
        val maxMs = 4_000L + text.length * 140L
        try {
            timedOut = withTimeoutOrNull(maxMs) { done.await() } == null
        } catch (e: CancellationException) {
            runCatching { t.stop() }
            throw e
        } finally {
            pending.remove(id)
        }
        // Engine báo lỗi (onError) = không đọc được, kể cả khi lệnh speak() đã nhận.
        if (failed.remove(id)) return false
        if (timedOut) Log.w(TAG, "Engine không báo đọc xong")
        return true
    }

    /**
     * Phát tiếng bíp dài [durationMs] ở mức to nhất có thể: tần số trầm (người già nghe tần số
     * thấp tốt hơn), làm "vuông" sóng để giàu âm bội nên nghe to hơn sóng sin thường.
     */
    suspend fun longBeep(durationMs: Int): Boolean = mutex.withLock {
        if (!::appContext.isInitialized) return@withLock false
        withVoiceVolume { longBeepLocked(durationMs) }
    }

    private suspend fun longBeepLocked(durationMs: Int): Boolean {
        val sr = 22050
        val n = sr * durationMs / 1000
        val fade = sr / 50 // 20 ms
        val drive = 2.5
        val norm = kotlin.math.tanh(drive)
        val samples = ShortArray(n) { i ->
            val t = i.toDouble() / sr
            val s = kotlin.math.tanh(drive * kotlin.math.sin(2 * Math.PI * 740.0 * t)) / norm
            val env = when {
                i < fade -> i.toDouble() / fade
                i > n - fade -> (n - i).toDouble() / fade
                else -> 1.0
            }
            (s * env * 0.97 * 32767).toInt().toShort()
        }
        return runCatching { play(Pcm(samples, sr, 1)) }.getOrDefault(false)
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
