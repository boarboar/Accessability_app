package com.example.myapplication

import android.R.attr.x
import android.R.attr.y
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.location.*
import java.util.*


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, OnAddressResolveListenerInterface {
    lateinit var tts: TextToSpeech
    var ttsEnabled = false
    lateinit var locator: Locator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        setContentView(R.layout.activity_main_circle2)
        //layoutButtons()

        tts = TextToSpeech(this, this)

        MapKitFactory.initialize(this)
        locator = Locator(this, this)

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

    fun layoutButtons() {
        val params = RelativeLayout.LayoutParams(80, 80) // size of button in dp
        val but6 = findViewById<Button>(R.id.button6)
        params.setMargins(128, 128, 0, 0);
        but6.layoutParams = params;
    }

    fun speak(text: String) {
        if (ttsEnabled) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    fun onHelp(view: View) {
        Toast.makeText(applicationContext, "Помощь", Toast.LENGTH_SHORT).show()
        speak("Рассказываем, как пользоваться приложением")
    }

    fun notImplemented(view: View) {
        Toast.makeText(applicationContext, "Нет функции", Toast.LENGTH_SHORT).show()
        speak("Нет функции")
    }

    fun goHome(view: View) {
        Toast.makeText(applicationContext, "Идем домой", Toast.LENGTH_SHORT).show()
        speak("Строим маршрут для движения домой")
    }

    fun goDrugstore(view: View) {
        Toast.makeText(applicationContext, "Идем в аптеку", Toast.LENGTH_SHORT).show()
        speak("Строим маршрут для движения в аптеку")
    }

    fun callDoctor(view: View) {
        Toast.makeText(applicationContext, "Звонить врачу", Toast.LENGTH_SHORT).show()
        speak("Набираем телефон врача")
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

    override fun onLocationResolve(address1: String, address2: String) {
        speak(address1 + ",   " + address2)
    }

    override fun onLocationError(error: String) {
        speak(error)
    }
}
