package com.show.animated_webp.frame

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.show.animated_webp.io.imp.Reader
import com.show.animated_webp.io.imp.Writer

/**
 * @Description: One frame in an animation
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-13
 */
abstract class Frame<R : Reader?, W : Writer?>(val reader: R) {
    var frameWidth = 0
    var frameHeight = 0
    var frameX = 0
    var frameY = 0
    var frameDuration = 0
    abstract fun draw(
        canvas: Canvas,
        paint: Paint,
        sampleSize: Int,
        reusedBitmap: Bitmap?,
        writer: W
    ): Bitmap?
}