package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yandex.mapkit.MapKitFactory
import java.util.*


// TODO
// Vibro - add logging and toast for testing
// Attach to real device
// Layout selector - Done
// Call number

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, OnAddressResolveListenerInterface {
    private lateinit var tts: TextToSpeech
    private var ttsEnabled = false
    private  lateinit var locator: Locator
    private lateinit var vibrator: Vibrator
    private lateinit var round_view: View
    private lateinit var square_view: View
    private val PERMISSIONS_REQUEST_CALL_PHONE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        //setContentView(R.layout.activity_main_circle2)

        round_view = layoutInflater.inflate(R.layout.activity_main_circle2, null)
        square_view = layoutInflater.inflate(R.layout.activity_main, null)
        setContentView(round_view)

        tts = TextToSpeech(this, this)

        MapKitFactory.initialize(this)
        locator = Locator(this, this)
        vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (!vibrator.hasVibrator()) {
            Log.e("VIB", "Has no vibrator")
        }

        requestCallPermission()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            //val result = tts.setLanguage(Locale.US)
            val result = tts.setLanguage(Locale("ru"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            } else {
                ttsEnabled = true
                Log.i("TTS", "Enabled")
            }
        } else {
            Log.e("TTS", "Initilization Failed!")
        }

        if (!ttsEnabled) {
            Toast.makeText(this@MainActivity, "Can not enable TTS!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "TTS enabled", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        //mapView.onStart()
        locator.subscribeToLocationUpdate()
    }

    override fun onStop() {
        super.onStop()
        locator.unsubscribeFromLocationUpdate()
        MapKitFactory.getInstance().onStop()
        //mapView.onStop()
    }

    public override fun onDestroy() {
        // Shutdown TTS
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.round_layout -> {
                setContentView(round_view)
                true
            }
            R.id.square_layout -> {
                setContentView(square_view);
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /*
    fun layoutButtons() {
        val params = RelativeLayout.LayoutParams(80, 80) // size of button in dp
        val but6 = findViewById<Button>(R.id.button6)
        params.setMargins(128, 128, 0, 0);
        but6.layoutParams = params;
    }
*/

    fun speak(text: String) {
        if (ttsEnabled) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    private fun createOneShotVibrationUsingVibrationEffect(milliseconds: Long) {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26
                Log.i("VIB", "Use API 26")
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        milliseconds,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                Log.i("VIB", "Use API < 26")
                // This method was deprecated in API level 26
                vibrator.vibrate(milliseconds)
            }
        }
    }


    private fun createWaveFormVibrationUsingVibrationEffectAndAmplitude() {
        if (vibrator.hasVibrator()) {
            val vibratePattern = longArrayOf(0, 400, 800, 600, 800, 800, 800, 1000)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26
                // -1 : Play exactly once
                Log.i("VIB", "Use API 26")
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

    private fun createWaveFormVibrationUsingVibrationEffect() {
        if (vibrator.hasVibrator()) {
            val vibratePattern = longArrayOf(0, 400, 800, 600, 800, 800, 800, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26
                // -1 : Play exactly once
                Log.i("VIB", "Use API 26")
                val effect = VibrationEffect.createWaveform(vibratePattern,-1)
                vibrator.vibrate(effect)
            } else {
                // This method was deprecated in API level 26
                Log.i("VIB", "Use API < 26")
                vibrator.vibrate(vibratePattern, -1);
            }
        }
    }

    private fun requestCallPermission() {
        if (ContextCompat.checkSelfPermission(this,
                "android.permission.CALL_PHONE"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            /*
            requestPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) */
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CALL_PHONE),
                PERMISSIONS_REQUEST_CALL_PHONE
            )
        }
    }

    fun onHelp(view: View) {
        Toast.makeText(applicationContext, "Помощь", Toast.LENGTH_SHORT).show()
        speak("Рассказываем, как пользоваться приложением")
    }

    fun notImplemented(view: View) {
        Toast.makeText(applicationContext, "Нет функции", Toast.LENGTH_SHORT).show()
        //speak("Нет функции")
        createWaveFormVibrationUsingVibrationEffect()
    }

    fun goHome(view: View) {
        Toast.makeText(applicationContext, "Идем домой", Toast.LENGTH_SHORT).show()
        speak("Строим маршрут для движения домой")
    }

    fun goDrugstore(view: View) {
        createOneShotVibrationUsingVibrationEffect(1000);
        Toast.makeText(applicationContext, "Идем в аптеку", Toast.LENGTH_SHORT).show()
        speak("Строим маршрут для движения в аптеку")
    }

    fun callDoctor(view: View) {
        val phone_number = "060"
        Toast.makeText(applicationContext, "Звонить врачу", Toast.LENGTH_SHORT).show()
        speak("Набираем телефон врача $phone_number")
        // Getting instance of Intent with action as ACTION_CALL
        // val phone_intent = Intent(Intent.ACTION_DIAL)
        val phone_intent = Intent(Intent.ACTION_CALL)
        // Set data of Intent through Uri by parsing phone number

        // Set data of Intent through Uri by parsing phone number
        phone_intent.data = Uri.parse("tel:$phone_number")

        // start Intent

        // start Intent
        startActivity(phone_intent)
    }

    fun whereAmI(view: View) {
        /*
        Toast.makeText(applicationContext, "Где я", Toast.LENGTH_SHORT).show()
        speak("Определем местоположение и смотрим, что есть рядом")
    */
        val msg = locator.requestAddress()
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
        speak(msg)
    }
/*
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        super.onKeyUp(keyCode, event)
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Toast.makeText(this@MainActivity, "Up working", Toast.LENGTH_SHORT).show()
            speak("Действие по кнопке вверх")
            return true
        }
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        super.onKeyDown(keyCode, event)
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Toast.makeText(this@MainActivity, "Down working", Toast.LENGTH_SHORT).show()
            speak("Действие по кнопке вниз")
            return true
        }
        return false
    }
*/

    override fun onLocationResolve(address1: String, address2: String) {
        speak(address1 + ",   " + address2)
    }

    override fun onLocationError(error: String) {
        speak(error)
    }
}
