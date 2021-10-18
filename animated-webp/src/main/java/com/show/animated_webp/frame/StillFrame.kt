package com.show.animated_webp.frame

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.show.animated_webp.decoder.WebPReader
import com.show.animated_webp.io.WebPWriter
import java.io.IOException

/**
 * @Description: StillFrame
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-13
 */
class StillFrame(reader: WebPReader, width: Int, height: Int) : Frame<WebPReader, WebPWriter>(reader) {


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
        var bitmap: Bitmap? = null
        try {
            try {
                bitmap = BitmapFactory.decodeStream(reader.toInputStream(), null, options)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                // Problem decoding into existing bitmap when on Android 4.2.2 & 4.3
                val optionsFixed = BitmapFactory.Options()
                optionsFixed.inJustDecodeBounds = false
                optionsFixed.inSampleSize = sampleSize
                optionsFixed.inMutable = true
                bitmap = BitmapFactory.decodeStream(reader.toInputStream(), null, optionsFixed)
            }
            assert(bitmap != null)
            paint.xfermode = null
            canvas.drawBitmap(bitmap!!, 0f, 0f, paint)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bitmap
    }

    init {
        frameWidth = width
        frameHeight = height
    }
}