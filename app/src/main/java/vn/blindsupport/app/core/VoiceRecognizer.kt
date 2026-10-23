package vn.blindsupport.app.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import kotlin.coroutines.resume

/**
 * Nhận dạng giọng nói tiếng Việt hoàn toàn offline bằng Vosk.
 *
 * Mỗi lần nghe chỉ cho phép một danh sách cụm từ cố định (ví dụ "nghe", "không", "gọi mẹ"),
 * nhờ vậy độ chính xác cao hơn nhiều so với nhận dạng câu tự do.
 */
object VoiceRecognizer {
    private const val TAG = "VoiceRecognizer"
    private const val SAMPLE_RATE = 16000f

    @Volatile private var model: Model? = null
    private val loadMutex = Mutex()
    private val listenMutex = Mutex()

    @Volatile var lastError: String? = null
        private set

    val isModelLoaded: Boolean get() = model != null

    fun preload(context: Context, scope: CoroutineScope) {
        scope.launch { ensureModel(context) }
    }

    suspend fun ensureModel(context: Context): Model? = loadMutex.withLock {
        model?.let { return@withLock it }
        val loaded = suspendCancellableCoroutine<Model?> { cont ->
            try {
                StorageService.unpack(
                    context.applicationContext, "model-vn", "model",
                    { m -> if (cont.isActive) cont.resume(m) },
                    { e ->
                        lastError = e.message
                        Log.e(TAG, "Không nạp được model", e)
                        if (cont.isActive) cont.resume(null)
                    }
                )
            } catch (e: Exception) {
                lastError = e.message
                if (cont.isActive) cont.resume(null)
            }
        }
        model = loaded
        loaded
    }

    fun hasMicPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /**
     * Bật micro nghe tối đa [timeoutMs]. Trả về câu nghe được đầu tiên mà [accept] chấp nhận,
     * hoặc null nếu hết giờ / lỗi micro.
     */
    suspend fun listen(
        context: Context,
        phrases: Collection<String>,
        timeoutMs: Long,
        accept: (String) -> Boolean = { true },
    ): String? = listenMutex.withLock {
        if (!hasMicPermission(context)) {
            lastError = "Chưa cấp quyền micro"
            return@withLock null
        }
        val m = ensureModel(context) ?: return@withLock null
        val grammar = JSONArray().apply {
            phrases.map { TextUtils.normalize(it) }.filter { it.isNotBlank() }.distinct().forEach { put(it) }
            put("[unk]")
        }.toString()

        val recognizer = try {
            Recognizer(m, SAMPLE_RATE, grammar)
        } catch (e: Exception) {
            Log.w(TAG, "Grammar lỗi, chuyển sang nghe tự do", e)
            try { Recognizer(m, SAMPLE_RATE) } catch (e2: Exception) { lastError = e2.message; return@withLock null }
        }
        val service = try {
            SpeechService(recognizer, SAMPLE_RATE)
        } catch (e: Exception) {
            lastError = "Không mở được micro: ${e.message}"
            recognizer.close()
            return@withLock null
        }

        try {
            suspendCancellableCoroutine<String?> { cont ->
                fun finish(text: String?) {
                    if (cont.isActive) cont.resume(text)
                }
                val listener = object : RecognitionListener {
                    override fun onPartialResult(hypothesis: String?) {}
                    override fun onResult(hypothesis: String?) {
                        val t = parse(hypothesis)
                        if (t.isNotBlank() && accept(t)) finish(t)
                    }
                    override fun onFinalResult(hypothesis: String?) {
                        val t = parse(hypothesis)
                        finish(if (t.isNotBlank() && accept(t)) t else null)
                    }
                    override fun onError(exception: Exception?) {
                        lastError = exception?.message
                        finish(null)
                    }
                    override fun onTimeout() { finish(null) }
                }
                if (!service.startListening(listener, timeoutMs.toInt())) finish(null)
                cont.invokeOnCancellation { runCatching { service.stop() } }
            }
        } finally {
            runCatching { service.stop() }
            runCatching { service.shutdown() }
            runCatching { recognizer.close() }
        }
    }

    private fun parse(hypothesis: String?): String {
        if (hypothesis.isNullOrBlank()) return ""
        val raw = runCatching { JSONObject(hypothesis).optString("text") }.getOrDefault("")
        return raw.replace("[unk]", " ").replace(Regex("\\s+"), " ").trim()
    }
}
