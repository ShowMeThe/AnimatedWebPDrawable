package com.show.animated_webp.chunk

/**
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-11
 */
class XMPChunk : BaseChunk() {
    companion object{
        val ID: Int = fourCCToInt("XMP ")
    }
}