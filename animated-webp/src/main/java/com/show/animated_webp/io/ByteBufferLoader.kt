package com.show.animated_webp.io

import com.show.animated_webp.io.imp.Loader
import com.show.animated_webp.io.imp.Reader
import java.io.IOException
import java.nio.ByteBuffer

/**
 * @Description: ByteBufferLoader
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-15
 */
abstract class ByteBufferLoader : Loader {
    abstract fun getByteBuffer(): ByteBuffer
    @Throws(IOException::class)
    override fun obtain(): Reader {
        return ByteBufferReader(getByteBuffer())
    }
}