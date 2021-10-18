package com.show.animated_webp.frame

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.WorkerThread
import com.show.animated_webp.io.imp.Loader
import com.show.animated_webp.io.imp.Reader
import com.show.animated_webp.io.imp.Writer
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport
import kotlin.collections.ArrayList

/**
 * @Description: Abstract Frame Animation Decoder
 * @Author: pengfei.zhou
 * @CreateDate: 2019/3/27
 */
abstract class FrameSeqDecoder<R : Reader, W : Writer>(
    private val mLoader: Loader, renderListener: RenderListener
) {
    private val taskId = FrameDecoderExecutor.getInstance().generateTaskId()
    private val workerHandler =
        Handler(FrameDecoderExecutor.getInstance().getLooper(taskId))
    protected var frames  = ArrayList<Frame<R, W>>()
    protected var frameIndex = -1
    private var playCount = 0
    private var loopLimit: Int? = null
    private val renderListeners: MutableSet<RenderListener> = HashSet()
    private val paused = AtomicBoolean(true)
    private val renderTask: Runnable = object : Runnable {
        override fun run() {
            if (DEBUG) {
                Log.d(TAG, "$this,run")
            }
            if (paused.get()) {
                return
            }
            if (canStep()) {
                val start = System.currentTimeMillis()
                val delay = step()
                val cost = System.currentTimeMillis() - start
                workerHandler.postDelayed(this, Math.max(0, delay - cost))
                for (renderListener in renderListeners) {
                    renderListener.onRender(frameBuffer)
                }
            } else {
                stop()
            }
        }
    }
    var sampleSize = 1
        protected set
    private val cacheBitmaps: MutableSet<Bitmap> = HashSet()
    private val cacheBitmapsLock = Any()
    protected var cachedCanvas: MutableMap<Bitmap, Canvas> = WeakHashMap()
    protected var frameBuffer: ByteBuffer? = null

    @Volatile
    protected var fullRect: Rect? = null
    private var mWriter: W? = writer
    private var mReader: R? = null

    /**
     * If played all the needed
     */
    private var finished = false

    private enum class State {
        IDLE, RUNNING, INITIALIZING, FINISHING
    }

    /**
     * @param loader         webp的reader
     * @param renderListener 渲染的回调
     */
    init {
        renderListeners.add(renderListener)
    }

    @Volatile
    private var mState = State.IDLE
    protected abstract val writer: W
    protected abstract fun getReader(reader: Reader?): R
    protected fun obtainBitmap(width: Int, height: Int): Bitmap? {
        synchronized(cacheBitmapsLock) {
            var ret: Bitmap? = null
            val iterator = cacheBitmaps.iterator()
            while (iterator.hasNext()) {
                val reuseSize = width * height * 4
                ret = iterator.next()
                if (ret.allocationByteCount >= reuseSize) {
                    iterator.remove()
                    if (ret.width != width || ret.height != height) {
                        ret.reconfigure(width, height, Bitmap.Config.ARGB_8888)
                    }
                    ret.eraseColor(0)
                    return ret
                }
            }
            try {
                val config = Bitmap.Config.ARGB_8888
                ret = Bitmap.createBitmap(width, height, config)
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            }
            return ret
        }
    }

    protected fun recycleBitmap(bitmap: Bitmap?) {
        synchronized(cacheBitmapsLock) {
            if (bitmap != null && !cacheBitmaps.contains(bitmap)) {
                cacheBitmaps.add(bitmap)
            }
        }
    }

    /**
     * 解码器的渲染回调
     */
    interface RenderListener {
        /**
         * 播放开始
         */
        fun onStart()

        /**
         * 帧播放
         */
        fun onRender(byteBuffer: ByteBuffer?)

        /**
         * 播放结束
         */
        fun onEnd()
    }

    fun addRenderListener(renderListener: RenderListener) {
        workerHandler.post { renderListeners.add(renderListener) }
    }

    fun removeRenderListener(renderListener: RenderListener) {
        workerHandler.post { renderListeners.remove(renderListener) }
    }

    fun stopIfNeeded() {
        workerHandler.post {
            if (renderListeners.size == 0) {
                stop()
            }
        }
    }

    val bounds: Rect
        get() {
            if (fullRect == null) {
                if (mState == State.FINISHING) {
                    Log.e(TAG, "In finishing,do not interrupt")
                }
                val thread = Thread.currentThread()
                workerHandler.post {
                    try {
                        if (fullRect == null) {
                            if (mReader == null) {
                                mReader = getReader(mLoader.obtain())
                            } else {
                                mReader!!.reset()
                            }
                            initCanvasBounds(read(mReader!!))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        fullRect = RECT_EMPTY
                    } finally {
                        LockSupport.unpark(thread)
                    }
                }
                LockSupport.park(thread)
            }
            return if (fullRect == null) RECT_EMPTY else fullRect!!
        }

    private fun initCanvasBounds(rect: Rect) {
        fullRect = rect
        frameBuffer =
            ByteBuffer.allocate((rect.width() * rect.height() / (sampleSize * sampleSize) + 1) * 4)
        if (mWriter == null) {
            mWriter = writer
        }
    }

    private val frameCount: Int
        private get() = frames.size

    /**
     * @return Loop Count defined in file
     */
    protected abstract val loopCount: Int
    fun start() {
        if (fullRect === RECT_EMPTY) {
            return
        }
        if (mState == State.RUNNING || mState == State.INITIALIZING) {
            Log.i(TAG, debugInfo() + " Already started")
            return
        }
        if (mState == State.FINISHING) {
            Log.e(TAG, debugInfo() + " Processing,wait for finish at " + mState)
        }
        if (DEBUG) {
            Log.i(TAG, debugInfo() + "Set state to INITIALIZING")
        }
        mState = State.INITIALIZING
        if (Looper.myLooper() == workerHandler.looper) {
            innerStart()
        } else {
            workerHandler.post { innerStart() }
        }
    }

    @WorkerThread
    private fun innerStart() {
        paused.compareAndSet(true, false)
        val start = System.currentTimeMillis()
        try {
            if (frames.size == 0) {
                try {
                    if (mReader == null) {
                        mReader = getReader(mLoader.obtain())
                    } else {
                        mReader!!.reset()
                    }
                    initCanvasBounds(read(mReader!!))
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        } finally {
            Log.i(
                TAG,
                debugInfo() + " Set state to RUNNING,cost " + (System.currentTimeMillis() - start)
            )
            mState = State.RUNNING
        }
        if (numPlays == 0 || !finished) {
            frameIndex = -1
            renderTask.run()
            for (renderListener in renderListeners) {
                renderListener.onStart()
            }
        } else {
            Log.i(TAG, debugInfo() + " No need to started")
        }
    }

    @WorkerThread
    private fun innerStop() {
        workerHandler.removeCallbacks(renderTask)
        frames.clear()
        synchronized(cacheBitmapsLock) {
            for (bitmap in cacheBitmaps) {
                if (bitmap != null && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            cacheBitmaps.clear()
        }
        if (frameBuffer != null) {
            frameBuffer = null
        }
        cachedCanvas.clear()
        try {
            if (mReader != null) {
                mReader!!.close()
                mReader = null
            }
            if (mWriter != null) {
                mWriter!!.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        release()
        if (DEBUG) {
            Log.i(TAG, debugInfo() + " release and Set state to IDLE")
        }
        mState = State.IDLE
        for (renderListener in renderListeners) {
            renderListener.onEnd()
        }
    }

    fun stop() {
        if (fullRect === RECT_EMPTY) {
            return
        }
        if (mState == State.FINISHING || mState == State.IDLE) {
            Log.i(TAG, debugInfo() + "No need to stop")
            return
        }
        if (mState == State.INITIALIZING) {
            Log.e(TAG, debugInfo() + "Processing,wait for finish at " + mState)
        }
        if (DEBUG) {
            Log.i(TAG, debugInfo() + " Set state to finishing")
        }
        mState = State.FINISHING
        if (Looper.myLooper() == workerHandler.looper) {
            innerStop()
        } else {
            workerHandler.post { innerStop() }
        }
    }

    private fun debugInfo(): String {
        return if (DEBUG) {
            String.format(
                "thread is %s, decoder is %s,state is %s",
                Thread.currentThread(),
                this@FrameSeqDecoder.toString(),
                mState.toString()
            )
        } else ""
    }

    protected abstract fun release()
    val isRunning: Boolean
        get() = mState == State.RUNNING || mState == State.INITIALIZING

    fun isPaused(): Boolean {
        return paused.get()
    }

    fun setLoopLimit(limit: Int) {
        loopLimit = limit
    }

    fun reset() {
        playCount = 0
        frameIndex = -1
        finished = false
    }

    fun pause() {
        workerHandler.removeCallbacks(renderTask)
        paused.compareAndSet(false, true)
    }

    fun resume() {
        paused.compareAndSet(true, false)
        workerHandler.removeCallbacks(renderTask)
        workerHandler.post(renderTask)
    }

    fun setDesiredSize(width: Int, height: Int): Boolean {
        var sampleSizeChanged = false
        val sample = getDesiredSample(width, height)
        if (sample != sampleSize) {
            sampleSize = sample
            sampleSizeChanged = true
            val tempRunning = isRunning
            workerHandler.removeCallbacks(renderTask)
            workerHandler.post {
                innerStop()
                try {
                    initCanvasBounds(read(getReader(mLoader.obtain())))
                    if (tempRunning) {
                        innerStart()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return sampleSizeChanged
    }

    protected fun getDesiredSample(desiredWidth: Int, desiredHeight: Int): Int {
        if (desiredWidth == 0 || desiredHeight == 0) {
            return 1
        }
        val radio = Math.min(bounds.width() / desiredWidth, bounds.height() / desiredHeight)
        var sample = 1
        while (sample * 2 <= radio) {
            sample *= 2
        }
        return sample
    }

    @Throws(IOException::class)
    protected abstract fun read(reader: R): Rect
    private val numPlays: Int
        get() = if (loopLimit != null) loopLimit!! else loopCount

    private fun canStep(): Boolean {
        if (!isRunning) {
            return false
        }
        if (frames.size == 0) {
            return false
        }
        if (numPlays <= 0) {
            return true
        }
        if (playCount < numPlays - 1) {
            return true
        } else if (playCount == numPlays - 1 && frameIndex < frameCount - 1) {
            return true
        }
        finished = true
        return false
    }

    @WorkerThread
    private fun step(): Long {
        frameIndex++
        if (frameIndex >= frameCount) {
            frameIndex = 0
            playCount++
        }
        val frame = getFrame(frameIndex) ?: return 0
        renderFrame(frame)
        return frame.frameDuration.toLong()
    }

    protected abstract fun renderFrame(frame: Frame<R, W>?)
    private fun getFrame(index: Int): Frame<R, W>? {
        return if (index < 0 || index >= frames.size) {
            null
        } else frames[index]
    }

    /**
     * Get Indexed frame
     *
     * @param index <0 means reverse from last index
     */
    @Throws(IOException::class)
    fun getFrameBitmap(index: Int): Bitmap? {
        var index = index
        if (mState != State.IDLE) {
            Log.e(TAG, debugInfo() + ",stop first")
            return null
        }
        mState = State.RUNNING
        paused.compareAndSet(true, false)
        if (frames.size == 0) {
            if (mReader == null) {
                mReader = getReader(mLoader.obtain())
            } else {
                mReader!!.reset()
            }
            initCanvasBounds(read(mReader!!))
        }
        if (index < 0) {
            index += frames.size
        }
        if (index < 0) {
            index = 0
        }
        frameIndex = -1
        while (frameIndex < index) {
            if (canStep()) {
                step()
            } else {
                break
            }
        }
        frameBuffer!!.rewind()
        val bitmap = Bitmap.createBitmap(
            bounds.width() / sampleSize,
            bounds.height() / sampleSize,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(frameBuffer)
        innerStop()
        return bitmap
    }

    val memorySize: Int
        get() {
            synchronized(cacheBitmapsLock) {
                var size = 0
                for (bitmap in cacheBitmaps) {
                    if (bitmap.isRecycled) {
                        continue
                    }
                    size +=
                        bitmap.allocationByteCount
                }
                if (frameBuffer != null) {
                    size += frameBuffer!!.capacity()
                }
                return size
            }
        }

    companion object {
        private val TAG = FrameSeqDecoder::class.java.simpleName
        private val RECT_EMPTY = Rect()
        const val DEBUG = false
    }


}