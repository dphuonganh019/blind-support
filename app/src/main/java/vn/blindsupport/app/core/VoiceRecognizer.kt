package vn.blindsupport.app.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
 * nhờ vậy độ chính xác cao hơn nhiều so với nhận dạng câu tự do. Những từ bộ nhận dạng
 * không chắc chắn (thường là tiếng ồn bị "đoán" thành một từ ngắn) bị bỏ qua.
 */
object VoiceRecognizer {
    private const val TAG = "VoiceRecognizer"
    private const val SAMPLE_RATE = 16000f
    /** Từ có độ tin cậy dưới mức này coi như tiếng ồn. */
    private const val MIN_CONF = 0.45

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

    @Volatile private var vocabulary: Set<String>? = null

    /**
     * Các chữ trong [text] mà model nhận dạng không có trong từ điển (nên không bao giờ nghe ra được).
     * Đọc từ điển từ file graph/words.txt của model đã giải nén. Không có file (hoặc model chưa nạp)
     * thì trả về danh sách rỗng.
     */
    fun unknownWords(context: Context, text: String): List<String> {
        if (model == null) return emptyList()
        val vocab = vocabulary ?: loadVocabulary(context).also { vocabulary = it }
        if (vocab.isEmpty()) return emptyList()
        return TextUtils.words(text).filter { it !in vocab }.distinct()
    }

    private fun loadVocabulary(context: Context): Set<String> {
        val dirs = listOfNotNull(context.getExternalFilesDir(null), context.filesDir)
        for (d in dirs) {
            val f = java.io.File(d, "model/graph/words.txt")
            if (!f.isFile) continue
            return runCatching {
                f.useLines { lines -> lines.mapNotNull { it.substringBefore(' ').takeIf { w -> w.isNotBlank() } }.toHashSet() }
            }.getOrDefault(emptySet())
        }
        return emptySet()
    }

    fun hasMicPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun grammarJson(phrases: Collection<String>): String = JSONArray().apply {
        phrases.map { TextUtils.normalize(it) }.filter { it.isNotBlank() }.distinct().forEach { put(it) }
        put("[unk]")
    }.toString()

    private fun newRecognizer(m: Model, phrases: Collection<String>): Recognizer? {
        val r = try {
            Recognizer(m, SAMPLE_RATE, grammarJson(phrases))
        } catch (e: Exception) {
            Log.w(TAG, "Grammar lỗi, chuyển sang nghe tự do", e)
            try { Recognizer(m, SAMPLE_RATE) } catch (e2: Exception) { lastError = e2.message; return null }
        }
        runCatching { r.setWords(true) }
        return r
    }

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
        val recognizer = newRecognizer(m, phrases) ?: return@withLock null
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

    /**
     * Nghe một dãy số được đọc (có thể ngắt quãng nhiều đoạn). Kết thúc khi người dùng im lặng
     * [silenceMs] sau khi đã đọc được ít nhất một chữ số, khi nói "xong", hoặc hết [maxMs].
     * Trả về các chữ số đã nghe được (có thể rỗng).
     */
    suspend fun listenDigits(
        context: Context,
        initial: String = "",
        silenceMs: Long = 3_000,
        maxMs: Long = 20_000,
    ): String = listenMutex.withLock {
        if (!hasMicPermission(context)) return@withLock initial
        val m = ensureModel(context) ?: return@withLock initial
        val phrases = DigitWords.WORDS + listOf("xong", "xong rồi", "hết", "gọi số", "số")
        val recognizer = newRecognizer(m, phrases) ?: return@withLock initial
        val service = try {
            SpeechService(recognizer, SAMPLE_RATE)
        } catch (e: Exception) {
            recognizer.close()
            return@withLock initial
        }
        val digits = StringBuilder(initial)
        var partialDigits = ""
        var lastActivity = System.currentTimeMillis()
        val start = lastActivity
        val done = CompletableDeferred<Unit>()
        try {
            val listener = object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    val p = runCatching { JSONObject(hypothesis ?: "").optString("partial") }.getOrDefault("")
                    val pd = DigitWords.toDigits(p)
                    if (pd != partialDigits) {
                        partialDigits = pd
                        lastActivity = System.currentTimeMillis()
                    }
                }
                override fun onResult(hypothesis: String?) {
                    val t = parse(hypothesis)
                    digits.append(DigitWords.toDigits(t))
                    partialDigits = ""
                    lastActivity = System.currentTimeMillis()
                    if (TextUtils.normalize(t).let { it.contains("xong") || it.contains("hết") }) done.complete(Unit)
                }
                override fun onFinalResult(hypothesis: String?) {
                    digits.append(DigitWords.toDigits(parse(hypothesis)))
                    partialDigits = ""
                    done.complete(Unit)
                }
                override fun onError(exception: Exception?) { done.complete(Unit) }
                override fun onTimeout() { done.complete(Unit) }
            }
            if (!service.startListening(listener, maxMs.toInt())) done.complete(Unit)
            coroutineScope {
                val watchdog = launch {
                    while (!done.isCompleted) {
                        delay(200)
                        val now = System.currentTimeMillis()
                        val heardSomething = digits.isNotEmpty() || partialDigits.isNotEmpty()
                        if (heardSomething && now - lastActivity > silenceMs) {
                            done.complete(Unit)
                        } else if (!heardSomething && now - start > 10_000) {
                            // 10 giây không đọc gì: thôi.
                            done.complete(Unit)
                        }
                    }
                }
                done.await()
                watchdog.cancel()
            }
        } finally {
            // Dừng micro; nếu còn phần nói dở (hết giờ giữa chừng) thì vẫn giữ.
            digits.append(partialDigits)
            runCatching { service.stop() }
            runCatching { service.shutdown() }
            runCatching { recognizer.close() }
        }
        digits.toString()
    }

    /** Lấy câu nhận được, bỏ các từ bộ nhận dạng không chắc chắn. */
    private fun parse(hypothesis: String?): String {
        if (hypothesis.isNullOrBlank()) return ""
        val obj = runCatching { JSONObject(hypothesis) }.getOrNull() ?: return ""
        val words = obj.optJSONArray("result")
        val raw = if (words != null && words.length() > 0) {
            buildList {
                for (i in 0 until words.length()) {
                    val w = words.optJSONObject(i) ?: continue
                    if (w.optDouble("conf", 1.0) >= MIN_CONF) add(w.optString("word"))
                }
            }.joinToString(" ")
        } else {
            obj.optString("text")
        }
        return raw.replace("[unk]", " ").replace(Regex("\\s+"), " ").trim()
    }
}

/** Đọc số bằng tiếng Việt: "không chín một hai" → "0912". */
object DigitWords {
    private val MAP = mapOf(
        "không" to '0', "linh" to '0', "lẻ" to '0',
        "một" to '1', "mốt" to '1',
        "hai" to '2',
        "ba" to '3',
        "bốn" to '4', "tư" to '4',
        "năm" to '5', "lăm" to '5',
        "sáu" to '6',
        "bảy" to '7', "bẩy" to '7',
        "tám" to '8',
        "chín" to '9',
    )
    val WORDS: List<String> = MAP.keys.toList()

    fun toDigits(text: String): String =
        TextUtils.words(text).mapNotNull { w -> MAP[w] ?: w.singleOrNull()?.takeIf { it.isDigit() } }
            .joinToString("")
}
