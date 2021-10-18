package com.show.animated_webp.drawable

import com.show.animated_webp.frame.FrameSeqDecoder
import com.show.animated_webp.io.imp.Loader
import com.show.animated_webp.decoder.WebPDecoder

/**
 * @Description: Animated webp drawable
 * @Author: pengfei.zhou
 * @CreateDate: 2019/3/27
 */
class WebPDrawable :
    FrameAnimationDrawable<WebPDecoder?> {

    constructor(provider: Loader) : super(provider)
    constructor(decoder: WebPDecoder) : super(decoder)


    override fun createFrameSeqDecoder(
        streamLoader: Loader,
        listener: FrameSeqDecoder.RenderListener
    ): WebPDecoder {
        return WebPDecoder(streamLoader, listener)
    }
}