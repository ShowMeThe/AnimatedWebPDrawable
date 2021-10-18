package com.show.animated_webp.io.imp

import java.io.IOException

/**
 * @Description: Loader
 * @Author: pengfei.zhou
 * @CreateDate: 2019-05-14
 */
interface Loader {
    @Throws(IOException::class)
    fun obtain(): Reader
}