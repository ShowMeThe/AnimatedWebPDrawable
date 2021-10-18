package com.show.animated_webp.io

import android.text.TextUtils

/**
 * @Description: WebPWriter
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-12
 */
class WebPWriter : ByteBufferWriter() {
    fun putUInt16(`val`: Int) {
        putByte((`val` and 0xff).toByte())
        putByte((`val` shr 8 and 0xff).toByte())
    }

    fun putUInt24(`val`: Int) {
        putByte((`val` and 0xff).toByte())
        putByte((`val` shr 8 and 0xff).toByte())
        putByte((`val` shr 16 and 0xff).toByte())
    }

    fun putUInt32(`val`: Int) {
        putByte((`val` and 0xff).toByte())
        putByte((`val` shr 8 and 0xff).toByte())
        putByte((`val` shr 16 and 0xff).toByte())
        putByte((`val` shr 24 and 0xff).toByte())
    }

    fun put1Based(i: Int) {
        putUInt24(i - 1)
    }

    fun putFourCC(fourCC: String) {
        if (TextUtils.isEmpty(fourCC) || fourCC.length != 4) {
            skip(4)
            return
        }
        putByte((fourCC[0].toInt() and 0xff).toByte())
        putByte((fourCC[1].toInt() and 0xff).toByte())
        putByte((fourCC[2].toInt() and 0xff).toByte())
        putByte((fourCC[3].toInt() and 0xff).toByte())
    }
}