package com.show.animated_webp.frame

import android.graphics.*
import com.show.animated_webp.chunk.ANMFChunk
import com.show.animated_webp.chunk.BaseChunk
import com.show.animated_webp.chunk.VP8XChunk
import com.show.animated_webp.decoder.WebPReader
import com.show.animated_webp.io.WebPWriter
import java.io.IOException

/**
 * @Description: AnimationFrame
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-12
 */
class AnimationFrame(reader: WebPReader, anmfChunk: ANMFChunk) :
    Frame<WebPReader, WebPWriter>(reader) {
    val imagePayloadOffset: Int
    val imagePayloadSize: Int
    val blendingMethod: Boolean
    val disposalMethod: Boolean
    private val useAlpha: Boolean
    private fun encode(writer: WebPWriter): Int {
        val vp8xPayloadSize = 10
        val size: Int =
            12 + (BaseChunk.Companion.CHUNCK_HEADER_OFFSET + vp8xPayloadSize) + imagePayloadSize
        writer.reset(size)
        // Webp Header
        writer.putFourCC("RIFF")
        writer.putUInt32(size)
        writer.putFourCC("WEBP")

        //VP8X
        writer.putUInt32(VP8XChunk.ID)
        writer.putUInt32(vp8xPayloadSize)
        writer.putByte((if (useAlpha) 0x10 else 0).toByte())
        writer.putUInt24(0)
        writer.put1Based(frameWidth)
        writer.put1Based(frameHeight)

        //ImageData
        try {
            reader!!.reset()
            reader.skip(imagePayloadOffset.toLong())
            reader.read(writer.toByteArray(), writer.position(), imagePayloadSize)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return size
    }

    override fun draw(
        canvas: Canvas,
        paint: Paint,
        sampleSize: Int,
        reusedBitmap: Bitmap?,
        writer: WebPWriter
    ): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize
        options.inMutable = true
        options.inBitmap = reusedBitmap
        val length = encode(writer)
        val bytes = writer.toByteArray()
        var bitmap: Bitmap?
        try {
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, length, options)
        } catch (e: IllegalArgumentException) {
            // Problem decoding into existing bitmap when on Android 4.2.2 & 4.3
            val optionsFixed = BitmapFactory.Options()
            optionsFixed.inJustDecodeBounds = false
            optionsFixed.inSampleSize = sampleSize
            optionsFixed.inMutable = true
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, length, optionsFixed)
        }
        assert(bitmap != null)
        if (blendingMethod) {
            paint.xfermode = PORTERDUFF_XFERMODE_SRC
        } else {
            paint.xfermode = PORTERDUFF_XFERMODE_SRC_OVER
        }
        canvas.drawBitmap(
            bitmap!!,
            frameX.toFloat() * 2 / sampleSize,
            frameY.toFloat() * 2 / sampleSize,
            paint
        )
        return bitmap
    }

    companion object {
        private val PORTERDUFF_XFERMODE_SRC_OVER = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        private val PORTERDUFF_XFERMODE_SRC = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }

    init {
        frameWidth = anmfChunk.frameWidth
        frameHeight = anmfChunk.frameHeight
        frameX = anmfChunk.frameX
        frameY = anmfChunk.frameY
        frameDuration = anmfChunk.frameDuration
        if (frameDuration == 0) {
            frameDuration = 100
        }
        blendingMethod = anmfChunk.blendingMethod()
        disposalMethod = anmfChunk.disposalMethod()
        imagePayloadOffset = anmfChunk.offset + BaseChunk.Companion.CHUNCK_HEADER_OFFSET + 16
        imagePayloadSize = anmfChunk.payloadSize - 16 + (anmfChunk.payloadSize and 1)
        useAlpha = anmfChunk.alphChunk != null
    }
}