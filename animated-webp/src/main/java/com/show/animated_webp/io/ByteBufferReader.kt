package com.show.animated_webp.io

import com.show.animated_webp.io.imp.Reader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * @Description: APNG4Android
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-14
 */
class ByteBufferReader(private val byteBuffer: ByteBuffer) : Reader {
    @Throws(IOException::class)
    override fun skip(total: Long): Long {
        byteBuffer.position((byteBuffer.position() + total).toInt())
        return total
    }

    @Throws(IOException::class)
    override fun peek(): Byte {
        return byteBuffer.get()
    }

    @Throws(IOException::class)
    override fun reset() {
        byteBuffer.position(0)
    }

    override fun position(): Int {
        return byteBuffer.position()
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, start: Int, byteCount: Int): Int {
        byteBuffer[buffer, start, byteCount]
        return byteCount
    }

    @Throws(IOException::class)
    override fun available(): Int {
        return byteBuffer.limit() - byteBuffer.position()
    }

    @Throws(IOException::class)
    override fun close() {
    }

    @Throws(IOException::class)
    override fun toInputStream(): InputStream? {
        return ByteArrayInputStream(byteBuffer.array())
    }

    init {
        byteBuffer.position(0)
    }
}