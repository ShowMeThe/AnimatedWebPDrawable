package com.show.animated_webp.io

import com.show.animated_webp.io.imp.Reader
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-11
 */
class StreamReader(`in`: InputStream) : FilterInputStream(`in`), Reader {
    private var position = 0
    @Throws(IOException::class)
    override fun peek(): Byte {
        val ret = read().toByte()
        position++
        return ret
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val ret = super.read(b, off, len)
        position += Math.max(0, ret)
        return ret
    }

    @Synchronized
    @Throws(IOException::class)
    override fun reset() {
        super.reset()
        position = 0
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        val ret = super.skip(n)
        position += ret.toInt()
        return ret
    }

    override fun position(): Int {
        return position
    }

    @Throws(IOException::class)
    override fun toInputStream(): InputStream? {
        return this
    }

    init {
        try {
            `in`.reset()
        } catch (e: IOException) {
            // e.printStackTrace();
        }
    }
}