package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources


class NavActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutInflater.inflate(R.layout.activity_nav, null))
    }

    fun onTurn(view: View) {
        Toast.makeText(applicationContext, "Turn", Toast.LENGTH_SHORT).show()
        val drawable = AppCompatResources.getDrawable(applicationContext, R.drawable.baseline_arrow_right)
        findViewById<ImageView>(R.id.imageView).setImageDrawable(drawable)
    }
}

