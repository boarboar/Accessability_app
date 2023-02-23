package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private var ttsEnabled = false
    private  lateinit var locator: Locator
    private lateinit var vibrator: Vibro
    private lateinit var round_view: View
    private lateinit var square_view: View
    private val geocodeApi = RetrofitHelper.getInstance().create(QuotesApi::class.java)
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
        locator = Locator(this/*, this*/)
        vibrator = Vibro(this)

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
        vibrator.createWaveFormVibrationUsingVibrationEffect()
    }

    fun goHome(view: View) {
        Toast.makeText(applicationContext, "Идем домой", Toast.LENGTH_SHORT).show()
        speak("Строим маршрут для движения домой")
    }

    fun goDrugstore(view: View) {
        //vibrator.createOneShotVibrationUsingVibrationEffect(1000);
        //Toast.makeText(applicationContext, "Идем в аптеку", Toast.LENGTH_SHORT).show()
        //speak("Строим маршрут для движения в аптеку")
        val msg = locator.search("аптека", {a1, a2 -> onLocationResolve(a1, a2) } , {error -> onLocationError(error)})
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
        speak(msg)
    }

    fun callDoctor(view: View) {
        val phone_number = "060"
        speak("Вы собираетесь набрать номер $phone_number; Нажмите кнопку ДА сверху чтобы подтвердтить")

        val callDialog = YNDialogFragment()
        callDialog.onYes = {
            speak("звоню")
            //val phone_intent = Intent(Intent.ACTION_CALL)
            val phone_intent = Intent(Intent.ACTION_CALL)
            phone_intent.data = Uri.parse("tel:$phone_number")
            startActivity(phone_intent)
        }
        callDialog.onNo = {
            speak("отбой")
        }
        callDialog.show(supportFragmentManager)
    }

    fun whereAmI(view: View) {
        /*
        Toast.makeText(applicationContext, "Где я", Toast.LENGTH_SHORT).show()
        speak("Определем местоположение и смотрим, что есть рядом")
    */
    /*
        // Yandex implementation
        val msg = locator.requestAddress1({a1, a2 -> onLocationResolve(a1, a2) } , {error -> onLocationError(error)})
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
        speak(msg)
     */
        // OSM implementation
        val loc = locator.location
        if (loc != null) {
            val pos = loc.position
            // launching a new coroutine
            GlobalScope.launch {
                var addressString : String? = null
                var addressStringFull : String? = null
                //"12 к2" -> "12 корп 2"
                try {
                    //val result = geocodeApi.getReverseGeocodeTest();
                    val result = geocodeApi.getReverseGeocode(pos.longitude, pos.latitude)
                    if (result != null && result.body() != null) {
                        // Checking the results
                        val address = result.body()!!.address
                        var house = address.house_number
                        if (house == null)
                            house = address.building
                        Log.d("GEO", result.body()!!.display_name)
                        Log.d("GEO", address.toString())
                        addressString = "${address.road} ${house}"
                        addressStringFull = result.body()!!.display_name
                    } else {
                        Log.e("GEO", "Failed with $result")
                    }
                } catch (e: Throwable) {
                    Log.e("GEO", e.toString())
                }
                // showing results in UI
                withContext(Dispatchers.Main) {
                    if (addressString != null) {
                        Toast.makeText(this@MainActivity, addressStringFull, Toast.LENGTH_LONG).show()
                        speak(addressString!!)
                    } else {
                        speak("Невозможно определить адрес")
                    }
                }
            }
        } else
            speak("Местоположение определяется, попробуйте повторить через несколько секунд")

    }

    fun onLocationResolve(address1: String, address2: String) {
        Toast.makeText(this@MainActivity, address1 + ",   " + address2, Toast.LENGTH_SHORT).show()
        speak(address1 + ",   " + address2)
    }

    fun onLocationError(error: String) {
        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
        speak(error)
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

}
