package com.example.webp

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.view.animation.Animation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import coil.imageLoader
import coil.load
import com.show.animated_webp.frame.AnimatedWebPDecoder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        /**
         * Load it by Coil's custom Decoder
         */
        val imageView = findViewById<ImageView>(R.id.ivWebp)
        imageView.load(R.drawable.ballon,imageLoader.newBuilder().apply {
            components {
                add(AnimatedWebPDecoder.Factory())
            }
        }.build())

        /**
         * Android Api >= 28
         */
        val ivWebp2 = findViewById<ImageView>(R.id.ivWebp2)
        val animationDrawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources,R.raw.ballon))
        val drawable = animationDrawable as AnimatedImageDrawable
        drawable.start()
        ivWebp2.setImageDrawable(drawable)



    }
}