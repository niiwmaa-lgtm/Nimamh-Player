package com.example.floatingplayer

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.nio.ByteBuffer

/** Cuts [startMs, endMs] out of an audio file without re-encoding and saves it to Music/FloatingPlayer. */
object AudioTrimmer {

    private class Target(val ext: String, val mime: String, val container: Int)

    /** Returns the saved location (for a message) or null when the format cannot be cut. */
    fun trim(ctx: Context, src: Uri, startMs: Long, endMs: Long, baseName: String): String? {
        val ex = MediaExtractor()
        var muxer: MediaMuxer? = null
        var pfd: ParcelFileDescriptor? = null
        var outUri: Uri? = null
        var started = false
        try {
            ex.setDataSource(ctx, src, null)

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

            val target = when (fmt.getString(MediaFormat.KEY_MIME)) {
                "audio/3gpp", "audio/amr-wb" ->
                    Target("3gp", "audio/3gpp", MediaMuxer.OutputFormat.MUXER_OUTPUT_3GPP)
                "audio/mp4a-latm" ->
                    Target("m4a", "audio/mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                "audio/opus" ->
                    Target("ogg", "audio/ogg", MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)
                else -> return null
            }

            val safe = baseName.replace(Regex("[^\\p{L}\\p{N}._-]"), "_").take(80)
            val name = "$safe.${target.ext}"

            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                put(MediaStore.Audio.Media.MIME_TYPE, target.mime)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/FloatingPlayer")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = ctx.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            outUri = uri
            pfd = ctx.contentResolver.openFileDescriptor(uri, "w") ?: return null

            val mx = MediaMuxer(pfd.fileDescriptor, target.container)
            muxer = mx
            val outTrack = mx.addTrack(fmt)
            mx.start()
            started = true

            val startUs = startMs * 1000
            val endUs = endMs * 1000
            ex.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buf = ByteBuffer.allocate(1 shl 20)
            val info = MediaCodec.BufferInfo()
            var written = 0
            while (true) {
                buf.clear()
                val n = ex.readSampleData(buf, 0)
                if (n < 0) break
                val t = ex.sampleTime
                if (t > endUs) break
                if (t >= startUs) {
                    info.set(0, n, t - startUs, ex.sampleFlags)
                    mx.writeSampleData(outTrack, buf, info)
                    written++
                }
                ex.advance()
            }

            mx.stop()
            started = false
            mx.release()
            muxer = null
            pfd.close()
            pfd = null

            if (written == 0) {
                ctx.contentResolver.delete(uri, null, null)
                return null
            }
            val done = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
            ctx.contentResolver.update(uri, done, null, null)
            return "Music/FloatingPlayer/$name"
        } catch (_: Exception) {
            val u = outUri
            if (u != null) {
                try {
                    ctx.contentResolver.delete(u, null, null)
                } catch (_: Exception) {
                }
            }
            return null
        } finally {
            try {
                if (started) muxer?.stop()
            } catch (_: Exception) {
            }
            try {
                muxer?.release()
            } catch (_: Exception) {
            }
            try {
                pfd?.close()
            } catch (_: Exception) {
            }
            try {
                ex.release()
            } catch (_: Exception) {
            }
        }
    }
}
