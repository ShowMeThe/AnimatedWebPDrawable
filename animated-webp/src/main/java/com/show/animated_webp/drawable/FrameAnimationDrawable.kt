package com.show.animated_webp.drawable

import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.show.animated_webp.drawable.FrameAnimationDrawable
import com.show.animated_webp.frame.FrameSeqDecoder
import com.show.animated_webp.frame.FrameSeqDecoder.RenderListener
import com.show.animated_webp.io.imp.Loader
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.*

/**
 * @Description: Frame animation drawable
 * @Author: pengfei.zhou
 * @CreateDate: 2019/3/27
 */
abstract class FrameAnimationDrawable<Decoder : FrameSeqDecoder<*, *>?> : Drawable,
    Animatable2Compat, RenderListener {
    private val paint = Paint()
    private val frameSeqDecoder: Decoder
    private val drawFilter: DrawFilter =
        PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()
    private val animationCallbacks: MutableSet<Animatable2Compat.AnimationCallback> = HashSet()
    private var bitmap: Bitmap? = null
    private val uiHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_ANIMATION_START -> for (animationCallback in animationCallbacks) {
                    animationCallback.onAnimationStart(this@FrameAnimationDrawable)
                }
                MSG_ANIMATION_END -> for (animationCallback in animationCallbacks) {
                    animationCallback.onAnimationEnd(this@FrameAnimationDrawable)
                }
            }
        }
    }
    private val invalidateRunnable = Runnable { invalidateSelf() }
    private var autoPlay = true
    private val obtainedCallbacks: MutableSet<WeakReference<Callback?>> = HashSet()
    private var noMeasure = false

    constructor(frameSeqDecoder: Decoder) {
        paint.isAntiAlias = true
        this.frameSeqDecoder = frameSeqDecoder
    }

    constructor(provider: Loader) {
        paint.isAntiAlias = true
        frameSeqDecoder = createFrameSeqDecoder(provider, this)
    }

    fun setAutoPlay(autoPlay: Boolean) {
        this.autoPlay = autoPlay
    }

    fun setNoMeasure(noMeasure: Boolean) {
        this.noMeasure = noMeasure
    }

    protected abstract fun createFrameSeqDecoder(
        streamLoader: Loader,
        listener: RenderListener
    ): Decoder

    /**
     * @param loopLimit <=0为无限播放,>0为实际播放次数
     */
    fun setLoopLimit(loopLimit: Int) {
        frameSeqDecoder!!.setLoopLimit(loopLimit)
    }

    fun reset() {
        if (bitmap != null && !bitmap!!.isRecycled) {
            bitmap!!.eraseColor(Color.TRANSPARENT)
        }
        frameSeqDecoder!!.reset()
    }

    fun pause() {
        frameSeqDecoder!!.pause()
    }

    fun resume() {
        frameSeqDecoder!!.resume()
    }

    val isPaused: Boolean
        get() = frameSeqDecoder!!.isPaused()

    override fun start() {
        if (frameSeqDecoder!!.isRunning) {
            frameSeqDecoder.stop()
        }
        frameSeqDecoder.reset()
        innerStart()
    }

    private fun innerStart() {
        if (FrameSeqDecoder.DEBUG) {
            Log.d(TAG, "$this,start")
        }
        frameSeqDecoder!!.addRenderListener(this)
        if (autoPlay) {
            frameSeqDecoder.start()
        } else {
            if (!frameSeqDecoder.isRunning) {
                frameSeqDecoder.start()
            }
        }
    }

    override fun stop() {
        innerStop()
    }

    private fun innerStop() {
        if (FrameSeqDecoder.DEBUG) {
            Log.d(TAG, "$this,stop")
        }
        frameSeqDecoder!!.removeRenderListener(this)
        if (autoPlay) {
            frameSeqDecoder.stop()
        } else {
            frameSeqDecoder.stopIfNeeded()
        }
    }

    override fun isRunning(): Boolean {
        return frameSeqDecoder!!.isRunning
    }

    override fun draw(canvas: Canvas) {
        if (bitmap == null || bitmap!!.isRecycled) {
            return
        }
        canvas.drawFilter = drawFilter
        canvas.drawBitmap(bitmap!!, matrix, paint)
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        val sampleSizeChanged = frameSeqDecoder!!.setDesiredSize(bounds.width(), bounds.height())
        matrix.setScale(
            1.0f * bounds.width() * frameSeqDecoder.sampleSize / frameSeqDecoder.bounds.width(),
            1.0f * bounds.height() * frameSeqDecoder.sampleSize / frameSeqDecoder.bounds.height()
        )
        if (sampleSizeChanged) bitmap = Bitmap.createBitmap(
            frameSeqDecoder.bounds.width() / frameSeqDecoder.sampleSize,
            frameSeqDecoder.bounds.height() / frameSeqDecoder.sampleSize,
            Bitmap.Config.ARGB_8888
        )
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun onStart() {
        Message.obtain(uiHandler, MSG_ANIMATION_START).sendToTarget()
    }

    override fun onRender(byteBuffer: ByteBuffer?) {
        if (!isRunning) {
            return
        }
        if (bitmap == null || bitmap!!.isRecycled) {
            bitmap = Bitmap.createBitmap(
                frameSeqDecoder!!.bounds.width() / frameSeqDecoder.sampleSize,
                frameSeqDecoder.bounds.height() / frameSeqDecoder.sampleSize,
                Bitmap.Config.ARGB_8888
            )
        }
        byteBuffer!!.rewind()
        if (byteBuffer.remaining() < bitmap!!.byteCount) {
            Log.e(TAG, "onRender:Buffer not large enough for pixels")
            return
        }
        bitmap!!.copyPixelsFromBuffer(byteBuffer)
        uiHandler.post(invalidateRunnable)
    }

    override fun onEnd() {
        Message.obtain(uiHandler, MSG_ANIMATION_END).sendToTarget()
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        hookRecordCallbacks()
        if (autoPlay) {
            if (FrameSeqDecoder.DEBUG) {
                Log.d(TAG, "$this,visible:$visible,restart:$restart")
            }
            if (visible) {
                if (!isRunning) {
                    innerStart()
                }
            } else if (isRunning) {
                innerStop()
            }
        }
        return super.setVisible(visible, restart)
    }

    override fun getIntrinsicWidth(): Int {
        return if (noMeasure) {
            -1
        } else try {
            frameSeqDecoder!!.bounds.width()
        } catch (exception: Exception) {
            0
        }
    }

    override fun getIntrinsicHeight(): Int {
        return if (noMeasure) {
            -1
        } else try {
            frameSeqDecoder!!.bounds.height()
        } catch (exception: Exception) {
            0
        }
    }

    override fun registerAnimationCallback(animationCallback: Animatable2Compat.AnimationCallback) {
        animationCallbacks.add(animationCallback)
    }

    override fun unregisterAnimationCallback(animationCallback: Animatable2Compat.AnimationCallback): Boolean {
        return animationCallbacks.remove(animationCallback)
    }

    override fun clearAnimationCallbacks() {
        animationCallbacks.clear()
    }

    val memorySize: Int
        get() {
            var size = frameSeqDecoder!!.memorySize
            if (bitmap != null && !bitmap!!.isRecycled) {
                size += if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    bitmap!!.allocationByteCount
                } else {
                    bitmap!!.byteCount
                }
            }
            return Math.max(1, size)
        }

    override fun getCallback(): Callback? {
        return super.getCallback()
    }

    private fun hookRecordCallbacks() {
        val lost: MutableList<WeakReference<Callback?>> = ArrayList()
        val callback = callback
        var recorded = false
        for (ref in obtainedCallbacks) {
            val cb = ref.get()
            if (cb == null) {
                lost.add(ref)
            } else {
                if (cb === callback) {
                    recorded = true
                } else {
                    cb.invalidateDrawable(this)
                }
            }
        }
        for (ref in lost) {
            obtainedCallbacks.remove(ref)
        }
        if (!recorded) {
            obtainedCallbacks.add(WeakReference(callback))
        }
    }

    override fun invalidateSelf() {
        super.invalidateSelf()
        for (ref in obtainedCallbacks) {
            val callback = ref.get()
            if (callback != null && callback !== getCallback()) {
                callback.invalidateDrawable(this)
            }
        }
    }

    companion object {
        private val TAG = FrameAnimationDrawable::class.java.simpleName
        private const val MSG_ANIMATION_START = 1
        private const val MSG_ANIMATION_END = 2
    }
}