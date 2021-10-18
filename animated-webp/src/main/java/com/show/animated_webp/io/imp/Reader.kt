package com.show.animated_webp.io.imp

import java.io.IOException
import java.io.InputStream

/**
 * @link {https://developers.google.com/speed/webp/docs/riff_container#terminology_basics}
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-11
 */
interface Reader {
    @Throws(IOException::class)
    fun skip(total: Long): Long

    @Throws(IOException::class)
    fun peek(): Byte

    @Throws(IOException::class)
    fun reset()
    fun position(): Int

    @Throws(IOException::class)
    fun read(buffer: ByteArray, start: Int, byteCount: Int): Int

    @Throws(IOException::class)
    fun available(): Int

    /**
     * close io
     */
    @Throws(IOException::class)
    fun close()

    @Throws(IOException::class)
    fun toInputStream(): InputStream?
}