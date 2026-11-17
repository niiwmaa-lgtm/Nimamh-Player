package com.example.floatingplayer

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/** Decodes an audio file and returns one loudness value (0..1) per [windowMs]. Run on a background thread. */
object WaveformExtractor {

    fun extract(ctx: Context, uri: Uri, windowMs: Int, cancelled: () -> Boolean): FloatArray? {
        val ex = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            ex.setDataSource(ctx, uri, null)

            var track = -1
            var fmt: MediaFormat? = null
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val m = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (m.startsWith("audio/")) {
                    track = i
                    fmt = f
                    break
                }
            }
            if (track < 0 || fmt == null) return null
            ex.selectTrack(track)

            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: return null
            val c = MediaCodec.createDecoderByType(mime)
            codec = c
            c.configure(fmt, null, null, 0)
            c.start()

            var sampleRate = if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 8000
            var channels = if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1
            var windowSamples = maxOf(1, sampleRate * windowMs / 1000 * channels)

            val info = MediaCodec.BufferInfo()
            val peaks = ArrayList<Float>()
            var curMax = 0
            var count = 0
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (cancelled()) return null

                if (!inputDone) {
                    val ii = c.dequeueInputBuffer(10_000)
                    if (ii >= 0) {
                        val buf = c.getInputBuffer(ii)
                        val n = if (buf != null) ex.readSampleData(buf, 0) else -1
                        if (n < 0) {
                            c.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            c.queueInputBuffer(ii, 0, n, ex.sampleTime, 0)
                            ex.advance()
                        }
                    }
                }

                val oi = c.dequeueOutputBuffer(info, 10_000)
                if (oi >= 0) {
                    val out = c.getOutputBuffer(oi)
                    if (out != null && info.size > 0) {
                        out.position(info.offset)
                        out.limit(info.offset + info.size)
                        val sb = out.order(ByteOrder.nativeOrder()).asShortBuffer()
                        while (sb.hasRemaining()) {
                            val s = abs(sb.get().toInt())
                            if (s > curMax) curMax = s
                            count++
                            if (count >= windowSamples) {
                                peaks.add(curMax / 32768f)
                                curMax = 0
                                count = 0
                            }
                        }
                    }
                    c.releaseOutputBuffer(oi, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                } else if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val nf = c.outputFormat
                    if (nf.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = nf.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    if (nf.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = nf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    windowSamples = maxOf(1, sampleRate * windowMs / 1000 * channels)
                }
            }
            if (count > 0) peaks.add(curMax / 32768f)
            if (peaks.isEmpty()) return null

            var mx = 0f
            for (p in peaks) if (p > mx) mx = p
            if (mx <= 0f) mx = 1f
            val out = FloatArray(peaks.size)
            for (i in peaks.indices) out[i] = sqrt(peaks[i] / mx)
            return out
        } catch (_: Exception) {
            return null
        } finally {
            try {
                codec?.stop()
            } catch (_: Exception) {
            }
            try {
                codec?.release()
            } catch (_: Exception) {
            }
            try {
                ex.release()
            } catch (_: Exception) {
            }
        }
    }
}
