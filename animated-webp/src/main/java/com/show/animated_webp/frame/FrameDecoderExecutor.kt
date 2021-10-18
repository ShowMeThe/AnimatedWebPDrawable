package com.show.animated_webp.frame

import android.os.HandlerThread
import android.os.Looper
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @Description: com.github.penfeizhou.animation.executor
 * @Author: pengfei.zhou
 * @CreateDate: 2019-11-21
 */
class FrameDecoderExecutor private constructor() {
    private val mHandlerThreadGroup = ArrayList<HandlerThread?>()
    private val counter = AtomicInteger(0)

    internal object Inner {
        val sInstance = FrameDecoderExecutor()
    }

    fun setPoolSize(size: Int) {
        sPoolNumber = size
    }

    fun getLooper(taskId: Int): Looper {
        val idx = taskId % sPoolNumber
        return if (idx >= mHandlerThreadGroup.size) {
            val handlerThread = HandlerThread("FrameDecoderExecutor-$idx")
            handlerThread.start()
            mHandlerThreadGroup.add(handlerThread)
            val looper = handlerThread.looper
            looper ?: Looper.getMainLooper()
        } else {
            if (mHandlerThreadGroup[idx] != null) {
                val looper = mHandlerThreadGroup[idx]!!.looper
                looper ?: Looper.getMainLooper()
            } else {
                Looper.getMainLooper()
            }
        }
    }

    fun generateTaskId(): Int {
        return counter.getAndIncrement()
    }

    companion object {
        private var sPoolNumber = 4
        fun getInstance(): FrameDecoderExecutor {
            return Inner.sInstance
        }
    }
}