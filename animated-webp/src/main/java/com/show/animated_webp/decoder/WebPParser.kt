package com.show.animated_webp.decoder

import android.content.Context
import com.show.animated_webp.chunk.*
import com.show.animated_webp.io.imp.Reader
import com.show.animated_webp.io.StreamReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * @Description: APNG4Android
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-11
 */
object WebPParser {
    fun isAWebP(filePath: String?): Boolean {
        var inputStream: InputStream? = null
        return try {
            inputStream = FileInputStream(filePath)
            isAWebP(StreamReader(inputStream))
        } catch (e: Exception) {
            false
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun isAWebP(context: Context, assetPath: String?): Boolean {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.assets.open(assetPath!!)
            isAWebP(StreamReader(inputStream))
        } catch (e: Exception) {
            false
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun isAWebP(context: Context, resId: Int): Boolean {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.resources.openRawResource(resId)
            isAWebP(StreamReader(inputStream))
        } catch (e: Exception) {
            false
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun isAWebP(`in`: Reader?): Boolean {
        val reader = if (`in` is WebPReader) `in` else WebPReader(`in`)
        try {
            if (!reader.matchFourCC("RIFF")) {
                return false
            }
            reader.skip(4)
            if (!reader.matchFourCC("WEBP")) {
                return false
            }
            while (reader.available() > 0) {
                val chunk = parseChunk(reader)
                if (chunk is VP8XChunk) {
                    return chunk.animation()
                }
            }
        } catch (e: IOException) {
            if (e !is FormatException) {
                e.printStackTrace()
            }
        }
        return false
    }

    @Throws(IOException::class)
    fun parse(reader: WebPReader): List<BaseChunk> {
        //@link {https://developers.google.com/speed/webp/docs/riff_container#webp_file_header}
        if (!reader.matchFourCC("RIFF")) {
            throw FormatException()
        }
        reader.skip(4)
        if (!reader.matchFourCC("WEBP")) {
            throw FormatException()
        }
        val chunks: MutableList<BaseChunk> = ArrayList()
        while (reader.available() > 0) {
            chunks.add(parseChunk(reader))
        }
        return chunks
    }

    @Throws(IOException::class)
    fun parseChunk(reader: WebPReader): BaseChunk {
        //@link {https://developers.google.com/speed/webp/docs/riff_container#riff_file_format}
        val offset = reader.position()
        val chunkFourCC = reader.fourCC
        val chunkSize = reader.uInt32
        val chunk: BaseChunk = when {
            VP8XChunk.ID == chunkFourCC -> {
                VP8XChunk()
            }
            ANIMChunk.ID == chunkFourCC -> {
                ANIMChunk()
            }
            ANMFChunk.ID == chunkFourCC -> {
                ANMFChunk()
            }
            ALPHChunk.ID == chunkFourCC -> {
                ALPHChunk()
            }
            VP8Chunk.ID == chunkFourCC -> {
                VP8Chunk()
            }
            VP8LChunk.ID == chunkFourCC -> {
                VP8LChunk()
            }
            ICCPChunk.ID == chunkFourCC -> {
                ICCPChunk()
            }
            XMPChunk.ID == chunkFourCC -> {
                XMPChunk()
            }
            EXIFChunk.ID == chunkFourCC -> {
                EXIFChunk()
            }
            else -> {
                BaseChunk()
            }
        }
        chunk.chunkFourCC = chunkFourCC
        chunk.payloadSize = chunkSize
        chunk.offset = offset
        chunk.parse(reader)
        return chunk
    }

    internal class FormatException : IOException("WebP Format error")
}