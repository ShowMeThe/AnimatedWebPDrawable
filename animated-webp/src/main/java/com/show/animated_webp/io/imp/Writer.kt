package com.show.animated_webp.io.imp

import java.io.IOException

/**
 * @Description: APNG4Android
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-12
 */
interface Writer {
    fun reset(size: Int)
    fun putByte(b: Byte)
    fun putBytes(b: ByteArray?)
    fun position(): Int
    fun skip(length: Int)
    fun toByteArray(): ByteArray

    @Throws(IOException::class)
    fun close()
}