package com.show.animated_webp.chunk

/**
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-11
 */
class VP8Chunk : BaseChunk() {
    companion object{
        val ID: Int = BaseChunk.Companion.fourCCToInt("VP8 ")
    }
}