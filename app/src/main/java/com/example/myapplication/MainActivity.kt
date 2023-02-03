package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.*
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import java.util.*


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, OnAddressResolveListenerInterface {
    lateinit var tts: TextToSpeech
    var ttsEnabled = false
    lateinit var locator: Locator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

    fun speak(text: String) {
        if (ttsEnabled) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
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

        if (locator.location != null) {
            val pos = locator.location!!.position
            val accuracy = locator.location!!.accuracy
            val msg = "Loc - " + pos.getLatitude() + "," + pos.getLongitude() + " (" +
                    accuracy?.toInt() + ")"
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "Не удалось определить", Toast.LENGTH_SHORT).show()
        }
    */
        val msg = locator.getAddress()
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

    override fun onAddressResolve(address1: String, address2: String) {
        speak(address1 + ",   " + address2)
    }
}
