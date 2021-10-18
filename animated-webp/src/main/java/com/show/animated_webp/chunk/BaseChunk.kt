package com.show.animated_webp.chunk

import android.text.TextUtils
import com.show.animated_webp.decoder.WebPReader
import java.io.IOException

/**
 * @Description: BaseChunk
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-11
 */
open class BaseChunk {
    var chunkFourCC = 0
    var payloadSize = 0
    var offset = 0

    @Throws(IOException::class)
    fun parse(reader: WebPReader) {
        val available = reader.available()
        innerParse(reader)
        val offset = available - reader.available()

        /**
         * Chunk Payload: Chunk Size bytes
         * The data payload. If Chunk Size is odd, a single padding byte -- that SHOULD be 0 -- is added.
         */
        val payloadSizePadded = payloadSize + (payloadSize and 1)
        if (offset > payloadSizePadded) {
            throw IOException("Out of chunk area")
        } else if (offset < payloadSizePadded) {
            reader.skip((payloadSizePadded - offset).toLong())
        }
    }

    /**
     * Parse chunk data here
     *
     * @param reader current reader
     */
    @Throws(IOException::class)
    open fun innerParse(reader: WebPReader) {
    }

    companion object {
        const val CHUNCK_HEADER_OFFSET = 8
        fun fourCCToInt(fourCC: String): Int {
            return if (TextUtils.isEmpty(fourCC) || fourCC.length != 4) {
                -0x45210001
            } else fourCC[0]
                .code and 0xff or (fourCC[1].code and 0xff shl 8) or (fourCC[2].code and 0xff shl 16) or (fourCC[3].code and 0xff shl 24)
        }
    }
}