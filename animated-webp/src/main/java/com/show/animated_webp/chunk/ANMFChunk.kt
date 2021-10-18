package com.show.animated_webp.chunk

import com.show.animated_webp.decoder.WebPParser
import com.show.animated_webp.decoder.WebPReader
import java.io.IOException

/**
 * ANMF chunk:
 * For animated images, this chunk contains information about a single frame.
 * If the Animation flag is not set, then this chunk SHOULD NOT be present.
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                      ChunkHeader('ANMF')                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                        Frame X                |             ...
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * ...          Frame Y            |   Frame Width Minus One     ...
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * ...             |           Frame Height Minus One              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                 Frame Duration                |  Reserved |B|D|
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Frame Data                            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-11
 */
class ANMFChunk : BaseChunk() {
    /**
     * Frame X: 24 bits (uint24)
     * The X coordinate of the upper left corner of the frame is Frame X * 2
     */
    var frameX = 0

    /**
     * Frame Y: 24 bits (uint24)
     * The Y coordinate of the upper left corner of the frame is Frame Y * 2
     */
    var frameY = 0

    /**
     * Frame Width Minus One: 24 bits (uint24)
     * The 1-based width of the frame. The frame width is 1 + Frame Width Minus One
     */
    var frameWidth = 0

    /**
     * Frame Height Minus One: 24 bits (uint24)
     * The 1-based height of the frame. The frame height is 1 + Frame Height Minus One
     */
    var frameHeight = 0

    /**
     * Frame Duration: 24 bits (uint24)
     * The time to wait before displaying the next frame, in 1 millisecond units.
     * Note the interpretation of frame duration of 0 (and often <= 10) is implementation defined.
     * Many tools and browsers assign a minimum duration similar to GIF.
     */
    var frameDuration = 0
    var flags: Byte = 0
    var alphChunk: ALPHChunk? = null
    var vp8Chunk: VP8Chunk? = null
    var vp8LChunk: VP8LChunk? = null
    @Throws(IOException::class)
    public override fun innerParse(reader: WebPReader) {
        val available = reader.available()
        frameX = reader.uInt24
        frameY = reader.uInt24
        frameWidth = reader.get1Based()
        frameHeight = reader.get1Based()
        frameDuration = reader.uInt24
        flags = reader.peek()
        val bounds = (available - payloadSize).toLong()
        while (reader.available() > bounds) {
            val chunk = WebPParser.parseChunk(reader)
            if (chunk is ALPHChunk) {
                assert(alphChunk == null)
                alphChunk = chunk
            } else if (chunk is VP8Chunk) {
                assert(vp8Chunk == null && vp8LChunk == null)
                vp8Chunk = chunk
            } else if (chunk is VP8LChunk) {
                assert(vp8Chunk == null && vp8LChunk == null)
                vp8LChunk = chunk
            }
        }
    }

    fun blendingMethod(): Boolean {
        return flags.toInt() and FLAG_BLENDING_METHOD == FLAG_BLENDING_METHOD
    }

    fun disposalMethod(): Boolean {
        return flags.toInt() and FLAG_DISPOSAL_METHOD == FLAG_DISPOSAL_METHOD
    }

    companion object {
        val ID: Int = fourCCToInt("ANMF")

        /**
         * Blending method (B): 1 bit
         * Indicates how transparent pixels of the current frame are to be blended with corresponding pixels of the previous canvas:
         *
         *
         * 0: Use alpha blending. After disposing of the previous frame, render the current frame on the canvas using alpha-blending (see below).
         * If the current frame does not have an alpha channel, assume alpha value of 255, effectively replacing the rectangle.
         *
         *
         * 1: Do not blend. After disposing of the previous frame,
         * render the current frame on the canvas by overwriting the rectangle covered by the current frame.
         */
        private const val FLAG_BLENDING_METHOD = 0x2

        /**
         * Disposal method (D): 1 bit
         * Indicates how the current frame is to be treated after it has been displayed (before rendering the next frame) on the canvas:
         *
         *
         * 0: Do not dispose. Leave the canvas as is.
         *
         *
         * 1: Dispose to background color. Fill the rectangle on the canvas covered by the current frame with background color specified in the ANIM chunk.
         */
        private const val FLAG_DISPOSAL_METHOD = 0x1
    }
}