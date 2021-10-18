package com.show.animated_webp.decoder

import android.graphics.*
import com.show.animated_webp.chunk.ANIMChunk
import com.show.animated_webp.chunk.ANMFChunk
import com.show.animated_webp.chunk.VP8XChunk
import com.show.animated_webp.frame.AnimationFrame
import com.show.animated_webp.frame.Frame
import com.show.animated_webp.frame.FrameSeqDecoder
import com.show.animated_webp.frame.StillFrame
import com.show.animated_webp.io.imp.Loader
import com.show.animated_webp.io.imp.Reader
import com.show.animated_webp.io.WebPWriter
import java.io.IOException

/**
 * @Description: Animated webp Decoder
 * @Author: pengfei.zhou
 * @CreateDate: 2019/3/27
 */
class WebPDecoder(loader: Loader, renderListener: RenderListener,) :
    FrameSeqDecoder<WebPReader, WebPWriter>(loader, renderListener) {


    private val mTransparentFillPaint: Paint = Paint()
    private var paint: Paint? = null
    protected override var loopCount = 0
        private set
    private var canvasWidth = 0
    private var canvasHeight = 0
    private var alpha = false
    private var backgroundColor = 0
    private var mWriter = WebPWriter()

    override val writer: WebPWriter
        get() = mWriter

    override fun getReader(reader: Reader?): WebPReader {
        return WebPReader(reader)
    }

    override fun release() {}

    @Throws(IOException::class)


    override fun read(reader: WebPReader): Rect {
        val chunks = WebPParser.parse(reader)
        var anim = false
        var vp8x = false
        for (chunk in chunks) {
            if (chunk is VP8XChunk) {
                canvasWidth = chunk.canvasWidth
                canvasHeight = chunk.canvasHeight
                alpha = chunk.alpha()
                vp8x = true
            } else if (chunk is ANIMChunk) {
                anim = true
                backgroundColor = chunk.backgroundColor
                loopCount = chunk.loopCount
            } else if (chunk is ANMFChunk) {
                frames.add(AnimationFrame(reader, chunk))
            }
        }
        if (!anim) {
            //静态图
            if (!vp8x) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(reader.toInputStream(), null, options)
                canvasWidth = options.outWidth
                canvasHeight = options.outHeight
            }
            frames.add(StillFrame(reader, canvasWidth, canvasHeight))
            loopCount = 1
        }
        paint = Paint()
        paint!!.isAntiAlias = true
        if (!alpha) {
            mTransparentFillPaint.color = backgroundColor
        }
        return Rect(0, 0, canvasWidth, canvasHeight)
    }

    override fun renderFrame(frame: Frame<WebPReader, WebPWriter>?) {
        if (frame == null) {
            return
        }
        val bitmap = obtainBitmap(fullRect!!.width() / sampleSize, fullRect!!.height() / sampleSize)
        var canvas = cachedCanvas[bitmap]
        if (canvas == null) {
            canvas = Canvas(bitmap!!)
            cachedCanvas[bitmap] = canvas
        }
        // 从缓存中恢复当前帧
        frameBuffer!!.rewind()
        bitmap!!.copyPixelsFromBuffer(frameBuffer)
        if (frameIndex == 0) {
            if (alpha) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC)
            } else {
                canvas.drawColor(backgroundColor, PorterDuff.Mode.SRC)
            }
        } else {
            val preFrame = frames[frameIndex - 1]
            //Dispose to background color. Fill the rectangle on the canvas covered by the current frame with background color specified in the ANIM chunk.
            if (preFrame is AnimationFrame
                && preFrame.disposalMethod
            ) {
                val left = preFrame.frameX.toFloat() * 2 / sampleSize.toFloat()
                val top = preFrame.frameY.toFloat() * 2 / sampleSize.toFloat()
                val right =
                    (preFrame.frameX * 2 + preFrame.frameWidth).toFloat() / sampleSize.toFloat()
                val bottom =
                    (preFrame.frameY * 2 + preFrame.frameHeight).toFloat() / sampleSize.toFloat()
                canvas.drawRect(left, top, right, bottom, mTransparentFillPaint)
            }
        }
        val inBitmap = obtainBitmap(frame.frameWidth / sampleSize, frame.frameHeight / sampleSize)
        recycleBitmap(frame.draw(canvas, paint!!, sampleSize, inBitmap, writer))
        recycleBitmap(inBitmap)
        frameBuffer!!.rewind()
        bitmap.copyPixelsToBuffer(frameBuffer)
        recycleBitmap(bitmap)
    }

    companion object {
        private val TAG = WebPDecoder::class.java.simpleName
    }

    /**
     * @param loader         webp stream loader
     * @param renderListener callback for rendering
     */
    init {
        mTransparentFillPaint.color = Color.TRANSPARENT
        mTransparentFillPaint.style = Paint.Style.FILL
        mTransparentFillPaint.xfermode =
            PorterDuffXfermode(PorterDuff.Mode.SRC)
    }
}