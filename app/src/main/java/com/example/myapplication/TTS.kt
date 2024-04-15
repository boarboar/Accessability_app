package com.example.myapplication

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import java.util.Locale


class TTS private constructor(context: Context) : TextToSpeech.OnInitListener {
    private val TAG = "TTS"
    private var tts = TextToSpeech(context, this)
    private var ttsEnabled = false
    private var muted = false
    companion object {
        @Volatile
        private var instance: TTS? = null
        fun init(context: Context) {
            instance = TTS(context)
        }
        fun getInstance(): TTS {
            return instance!!
        }
        fun stop() {
            instance?.stop()
        }
        fun speak(text: String) {
            instance?.speak(text)
        }
        fun mute(mute : Boolean) {
            instance?.let {
                it.muted = mute
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            //val result = tts.setLanguage(Locale.US)
            val result = tts.setLanguage(Locale("ru"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "The Language specified is not supported!")
            } else {
                ttsEnabled = true
                Log.i(TAG, "Enabled ($status)")
            }
        } else {
            Log.e(TAG, "Initilization failed with $status !")
        }
    /*
        if (!ttsEnabled) {
            Toast.makeText(this@MainActivity, "Can not enable TTS!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "TTS enabled", Toast.LENGTH_SHORT).show()
        }
        */
    }

    fun stop() {
        tts.stop()
        tts.shutdown()
        Log.e(TAG, "Stopped")
    }

    fun speak(text: String) {
        if (ttsEnabled && !muted) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }
}