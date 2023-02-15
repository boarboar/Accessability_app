package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

class Vibro(val activity: Activity) {
    private val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val TAG = "VIB"
    init {
        if (!vibrator.hasVibrator()) {
            Log.e(TAG, "Has no vibrator")
        }
    }
    fun createOneShotVibrationUsingVibrationEffect(milliseconds: Long) {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26
                Log.i(TAG, "Use API 26")
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        milliseconds,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                Log.i(TAG, "Use API < 26")
                // This method was deprecated in API level 26
                vibrator.vibrate(milliseconds)
            }
        }
    }


    fun createWaveFormVibrationUsingVibrationEffectAndAmplitude() {
        if (vibrator.hasVibrator()) {
            val vibratePattern = longArrayOf(0, 400, 800, 600, 800, 800, 800, 1000)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26
                // -1 : Play exactly once
                Log.i(TAG, "Use API 26")
                if (vibrator.hasAmplitudeControl()) {
                    val effect = VibrationEffect.createWaveform(vibratePattern, amplitudes, -1)
                    vibrator.vibrate(effect)
                } else {
                    Log.e("VIB", "No Amp control")
                }
            } else {
                // This method was deprecated in API level 26
                Log.i("VIB", "Use API < 26")
                vibrator.vibrate(vibratePattern, -1);
            }
        }
    }

    fun createWaveFormVibrationUsingVibrationEffect() {
        if (vibrator.hasVibrator()) {
            val vibratePattern = longArrayOf(0, 400, 800, 600, 800, 800, 800, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26
                // -1 : Play exactly once
                Log.i(TAG, "Use API 26")
                val effect = VibrationEffect.createWaveform(vibratePattern,-1)
                vibrator.vibrate(effect)
            } else {
                // This method was deprecated in API level 26
                Log.i(TAG, "Use API < 26")
                vibrator.vibrate(vibratePattern, -1);
            }
        }
    }

}