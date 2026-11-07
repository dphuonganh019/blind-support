package vn.blindsupport.app.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Nghe một câu lệnh của người dùng.
 *
 * Ưu tiên bộ nhận dạng giọng nói của Google có sẵn trên máy (nghe tiếng Việt, tên người và
 * chữ số chính xác hơn rất nhiều). Nếu máy không có, không có mạng mà chưa tải gói tiếng Việt
 * offline, hoặc bị lỗi, thì dùng bộ nhận dạng offline Vosk như trước.
 */
object SpeechInput {
    private const val TAG = "SpeechInput"
    private const val GOOGLE_APP = "com.google.android.googlequicksearchbox"

    /** Bộ nhận dạng Google lỗi gần đây (thường do mất mạng): tạm dùng Vosk tới thời điểm này. */
    @Volatile private var systemDisabledUntil = 0L

    /** Bộ nhận dạng dùng ở lần nghe gần nhất ("Google" hoặc "offline"). */
    @Volatile var lastEngine: String = ""
        private set

    fun systemAvailable(context: Context): Boolean =
        runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    /**
     * Nghe một câu. Trả về các cách nghe (cách tốt nhất trước); danh sách rỗng nếu không nghe
     * được gì. [voskPhrases]: danh sách cụm cho bộ nhận dạng offline. [hints]: tên người, giúp
     * bộ nhận dạng Google nghe đúng tên (Android 13 trở lên).
     */
    suspend fun listen(
        context: Context,
        voskPhrases: Collection<String>,
        hints: List<String> = emptyList(),
        maxMs: Long = 12_000,
    ): List<String> {
        if (System.currentTimeMillis() > systemDisabledUntil && systemAvailable(context)) {
            when (val r = listenSystem(context, hints, maxMs)) {
                is SystemResult.Heard -> {
                    lastEngine = "Google"
                    return r.texts
                }
                SystemResult.Silence -> {
                    lastEngine = "Google"
                    return emptyList()
                }
                is SystemResult.Failed -> {
                    // Mất mạng / chưa có tiếng Việt: 3 phút tới dùng bộ nhận dạng offline cho đỡ chờ.
                    if (r.lasting) systemDisabledUntil = System.currentTimeMillis() + 3 * 60_000L
                }
            }
        }
        lastEngine = "offline"
        return listOfNotNull(VoiceRecognizer.listenUtterance(context, voskPhrases))
    }

    private sealed class SystemResult {
        data class Heard(val texts: List<String>) : SystemResult()
        object Silence : SystemResult()
        /** [lasting]: lỗi sẽ còn lặp lại (mất mạng, không có tiếng Việt, thiếu quyền). */
        data class Failed(val lasting: Boolean) : SystemResult()
    }

    /** Ưu tiên dịch vụ nhận dạng của Google (máy Samsung có thể đặt mặc định là của Samsung). */
    private fun createRecognizer(context: Context): SpeechRecognizer {
        val services = runCatching {
            context.packageManager.queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0)
        }.getOrDefault(emptyList())
        val google = services.firstOrNull { it.serviceInfo?.packageName == GOOGLE_APP }?.serviceInfo
        return if (google != null) {
            SpeechRecognizer.createSpeechRecognizer(context, ComponentName(google.packageName, google.name))
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    private suspend fun listenSystem(context: Context, hints: List<String>, maxMs: Long): SystemResult {
        val recognizer = try {
            createRecognizer(context)
        } catch (e: Exception) {
            Log.w(TAG, "Không tạo được bộ nhận dạng", e)
            return SystemResult.Failed(true)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Chờ người lớn tuổi nói chậm, ngập ngừng.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1_500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2_000L)
            if (Build.VERSION.SDK_INT >= 33 && hints.isNotEmpty()) {
                putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, ArrayList(hints.distinct().take(100)))
            }
        }
        try {
            val result = withTimeoutOrNull(maxMs + 4_000) {
                suspendCancellableCoroutine<SystemResult> { cont ->
                    fun done(r: SystemResult) {
                        if (cont.isActive) cont.resume(r)
                    }
                    recognizer.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                        override fun onResults(results: Bundle?) {
                            val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
                            done(if (texts.isEmpty()) SystemResult.Silence else SystemResult.Heard(texts))
                        }
                        override fun onError(error: Int) {
                            Log.w(TAG, "Lỗi nhận dạng: $error")
                            done(
                                when (error) {
                                    SpeechRecognizer.ERROR_NO_MATCH,
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SystemResult.Silence
                                    // Mạng (1, 2, 4, 11), không hỗ trợ tiếng Việt (12, 13), thiếu quyền (9).
                                    1, 2, 4, 9, 11, 12, 13 -> SystemResult.Failed(true)
                                    else -> SystemResult.Failed(false)
                                }
                            )
                        }
                    })
                    cont.invokeOnCancellation { runCatching { recognizer.cancel() } }
                    try {
                        recognizer.startListening(intent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Không bắt đầu nghe được", e)
                        done(SystemResult.Failed(false))
                    }
                }
            }
            if (result == null) runCatching { recognizer.cancel() }
            return result ?: SystemResult.Silence
        } finally {
            runCatching { recognizer.destroy() }
        }
    }
}
