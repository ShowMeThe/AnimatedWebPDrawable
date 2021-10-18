package com.show.animated_webp.decoder

import android.text.TextUtils
import com.show.animated_webp.io.FilterReader
import com.show.animated_webp.io.imp.Reader
import java.io.IOException

/**
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-11
 */
class WebPReader(reader: Reader?) : FilterReader(reader) {
    /**
     * @return uint16 A 16-bit, little-endian, unsigned integer.
     */
    @get:Throws(IOException::class)
    val uInt16: Int
        get() {
            val buf = ensureBytes()
            read(buf, 0, 2)
            return buf[0].toInt() and 0xff or (buf[1].toInt() and 0xff) shl 8
        }

    /**
     * @return uint24 A 24-bit, little-endian, unsigned integer.
     */
    @get:Throws(IOException::class)
    val uInt24: Int
        get() {
            val buf = ensureBytes()
            read(buf, 0, 3)
            return buf[0].toInt() and 0xff or (buf[1].toInt() and 0xff shl 8) or (buf[2].toInt() and 0xff shl 16)
        }

    /**
     * @return uint32 A 32-bit, little-endian, unsigned integer.
     */
    @get:Throws(IOException::class)
    val uInt32: Int
        get() {
            val buf = ensureBytes()
            read(buf, 0, 4)
            return buf[0].toInt() and 0xff or (buf[1].toInt() and 0xff shl 8) or (buf[2].toInt() and 0xff shl 16) or (buf[3].toInt() and 0xff shl 24)
        }

    /**
     * @return FourCC A FourCC (four-character code) is a uint32 created by concatenating four ASCII characters in little-endian order.
     */
    @get:Throws(IOException::class)
    val fourCC: Int
        get() {
            val buf = ensureBytes()
            read(buf, 0, 4)
            return buf[0].toInt() and 0xff or (buf[1].toInt() and 0xff shl 8) or (buf[2].toInt() and 0xff shl 16) or (buf[3].toInt() and 0xff shl 24)
        }

    /**
     * @return 1-based An unsigned integer field storing values offset by -1. e.g., Such a field would store value 25 as 24.
     */
    @Throws(IOException::class)
    fun get1Based(): Int {
        return uInt24 + 1
    }

    /**
     * @return read FourCC and match chars
     */
    @Throws(IOException::class)
    fun matchFourCC(chars: String): Boolean {
        if (TextUtils.isEmpty(chars) || chars.length != 4) {
            return false
        }
        val fourCC = fourCC
        for (i in 0..3) {
            if (fourCC shr i * 8 and 0xff != chars[i].toInt()) {
                return false
            }
        }
        return true
    }

    companion object {
        private val __intBytes = ThreadLocal<ByteArray>()
        protected fun ensureBytes(): ByteArray {
            var bytes = __intBytes.get()
            if (bytes == null) {
                bytes = ByteArray(4)
                __intBytes.set(bytes)
            }
            return bytes
        }
    }
}