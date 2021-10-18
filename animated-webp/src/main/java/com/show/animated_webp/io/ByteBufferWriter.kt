package com.show.animated_webp.io

import com.show.animated_webp.io.imp.Writer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @Description: ByteBufferWriter
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-12
 */
open class ByteBufferWriter : Writer {
    protected var byteBuffer: ByteBuffer? = null
    override fun putByte(b: Byte) {
        byteBuffer!!.put(b)
    }

    override fun putBytes(b: ByteArray?) {
        byteBuffer!!.put(b)
    }

    override fun position(): Int {
        return byteBuffer!!.position()
    }

    override fun skip(length: Int) {
        byteBuffer!!.position(length + position())
    }

    override fun toByteArray(): ByteArray {
        return byteBuffer!!.array()
    }

    override fun close() {}
    override fun reset(size: Int) {
        if (byteBuffer == null || size > byteBuffer!!.capacity()) {
            byteBuffer = ByteBuffer.allocate(size)
            byteBuffer?.order(ByteOrder.LITTLE_ENDIAN)
        }
        byteBuffer!!.clear()
    }

    init {
        reset(10 * 1024)
    }
}