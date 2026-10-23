package vn.blindsupport.app.core

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh

/** Âm thanh PCM 16-bit đã xử lý, sẵn sàng để phát. */
class Pcm(val samples: ShortArray, val sampleRate: Int, val channels: Int) {
    val frames: Int get() = samples.size / channels
}

/**
 * Làm giọng đọc to hơn mức tối đa của máy, cho người nghe kém:
 *  1. Chuẩn hóa: đẩy chỗ to nhất của câu lên sát mức tối đa.
 *  2. Nén động: nâng các âm nhỏ (phụ âm, cuối câu) lên tới [boost] lần, trong khi giữ đỉnh
 *     không vượt ngưỡng. Tai người cảm nhận to hơn rõ rệt mà ít bị rè.
 *  3. Giới hạn mềm: bo tròn đỉnh còn sót để không méo tiếng chát chúa.
 */
object AudioBooster {

    fun readWav(file: File): Pcm? {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        if (bytes.size < 44) return null
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (String(bytes, 0, 4, Charsets.US_ASCII) != "RIFF" ||
            String(bytes, 8, 4, Charsets.US_ASCII) != "WAVE"
        ) return null

        var pos = 12
        var channels = 1
        var sampleRate = 22050
        var bits = 16
        var format = 1
        var dataStart = -1
        var dataLen = 0
        while (pos + 8 <= bytes.size) {
            val id = String(bytes, pos, 4, Charsets.US_ASCII)
            val size = bb.getInt(pos + 4)
            val body = pos + 8
            if (id == "fmt " && body + 16 <= bytes.size) {
                format = bb.getShort(body).toInt() and 0xFFFF
                channels = bb.getShort(body + 2).toInt() and 0xFFFF
                sampleRate = bb.getInt(body + 4)
                bits = bb.getShort(body + 14).toInt() and 0xFFFF
            } else if (id == "data") {
                dataStart = body
                val remaining = bytes.size - body
                dataLen = if (size <= 0 || size > remaining) remaining else size
                break
            }
            if (size < 0 || body + size > bytes.size) break
            pos = body + size + (size and 1)
        }
        if (dataStart < 0 || format != 1 || bits != 16 || channels !in 1..2 || sampleRate <= 0) return null
        val count = dataLen / 2
        val sbuf = ByteBuffer.wrap(bytes, dataStart, count * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val out = ShortArray(min(count, sbuf.remaining()))
        sbuf.get(out)
        return Pcm(out, sampleRate, channels)
    }

    fun process(pcm: Pcm, boost: Int): Pcm {
        val n = pcm.samples.size
        if (n == 0) return pcm
        val x = FloatArray(n) { pcm.samples[it] / 32768f }

        // 1. Chuẩn hóa
        var peak = 0f
        for (v in x) peak = max(peak, abs(v))
        if (peak < 1e-4f) return pcm
        val norm = 0.95f / peak
        for (i in 0 until n) x[i] *= norm

        // 2. Nén động
        if (boost > 1) {
            val sr = pcm.sampleRate * pcm.channels
            // Thời gian nhả 8ms: đã đo trên giọng nói tiếng Việt, mức 2/3/4 cho to hơn khoảng
            // +4 / +6 / +8 dB so với chỉ chuẩn hóa; mức 4 bắt đầu hơi rè.
            val envRelease = exp(-1.0 / (0.008 * sr)).toFloat()
            val gainRelease = envRelease
            val target = 0.92f
            val maxGain = when (boost) {
                2 -> 2f
                3 -> 4f
                else -> 8f
            }
            var env = 0f
            var g = 1f
            for (i in 0 until n) {
                val a = abs(x[i])
                env = max(a, env * envRelease)
                // Khoảng lặng gần như tuyệt đối: không khuếch đại tiếng xì nền.
                val desired = if (env < 0.004f) 1f else min(maxGain, max(0.5f, target / env))
                g = if (desired < g) desired else desired + (g - desired) * gainRelease
                x[i] *= g
            }
        }

        // 3. Giới hạn mềm và đổi lại sang 16-bit
        val out = ShortArray(n)
        for (i in 0 until n) {
            out[i] = (softLimit(x[i]) * 32767f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return Pcm(out, pcm.sampleRate, pcm.channels)
    }

    private fun softLimit(v: Float): Float {
        val a = abs(v)
        if (a <= 0.85f) return v
        val limited = 0.85f + 0.14f * tanh(((a - 0.85f) / 0.14f).toDouble()).toFloat()
        return if (v < 0) -limited else limited
    }
}
