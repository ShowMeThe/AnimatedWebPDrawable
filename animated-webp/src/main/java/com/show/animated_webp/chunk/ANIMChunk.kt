package com.show.animated_webp.chunk

import com.show.animated_webp.decoder.WebPReader
import java.io.IOException

/**
 * For an animated image, this chunk contains the global parameters of the animation.
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                      ChunkHeader('ANIM')                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                       Background Color                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          Loop Count           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-11
 */
class ANIMChunk : BaseChunk() {
    /**
     * The default background color of the canvas in [Blue, Green, Red, Alpha] byte order.
     * This color MAY be used to fill the unused space on the canvas around the frames, as well as the transparent pixels of the first frame.
     * Background color is also used when disposal method is 1.
     */
    var backgroundColor = 0

    /**
     * The number of times to loop the animation. 0 means infinitely.
     */
    var loopCount = 0
    @Throws(IOException::class)
    public override fun innerParse(reader: WebPReader) {
        backgroundColor = reader.uInt32
        loopCount = reader.uInt16
    }

    companion object {
        val ID: Int = fourCCToInt("ANIM")
    }
}