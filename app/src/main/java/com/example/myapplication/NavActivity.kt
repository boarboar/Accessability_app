package com.example.myapplication

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources


class NavActivity : AppCompatActivity() {
    private val icons = arrayOf(R.drawable.baseline_arrow_upward, R.drawable.baseline_arrow_right,
        R.drawable.baseline_arrow_downward, R.drawable.baseline_arrow_left, )
    private val testText = arrayOf("10", "100", "500", "1000")
    private lateinit var drawables: ArrayList<Drawable>
    private var dir = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutInflater.inflate(R.layout.activity_nav, null))
        drawables = icons.map { AppCompatResources.getDrawable(applicationContext, it) } as ArrayList<Drawable>
    }

    fun onTurn(view: View) {
        //Toast.makeText(applicationContext, "Turn", Toast.LENGTH_SHORT).show()
        dir = (dir + 1) % 4
        findViewById<ImageView>(R.id.imageView).setImageDrawable(drawables[dir])
        findViewById<TextView>(R.id.textView).text = testText[dir]
    }
}

