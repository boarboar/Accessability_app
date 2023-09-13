package com.example.myapplication

import android.Manifest
//import android.content.Context
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
import androidx.lifecycle.ViewModelProvider
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private val PERMISSIONS_REQUEST_CALL_PHONE = 2
    private val HOME_LOCATION = Point(59.920499, 30.497943)
    private val VOL_UP_DELAY = 1000L
    private lateinit var tts: TextToSpeech
    private var ttsEnabled = false
    private  lateinit var locator: Locator
    private lateinit var vibrator: Vibro
    private lateinit var round_view: View
    private lateinit var square_view: View
    private var lastPressedUp : Long = 0 // in millis
    private lateinit var viewModel: MainViewModel
    private var ynDialog: YNDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        //setContentView(R.layout.activity_main_circle2)

        round_view = layoutInflater.inflate(R.layout.activity_main_circle2, null)
        square_view = layoutInflater.inflate(R.layout.activity_main, null)
        setContentView(round_view)

        tts = TextToSpeech(this, this)

        MapKitFactory.initialize(this)
        locator = Locator(this)
        vibrator = Vibro(this)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        requestCallPermission()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            //val result = tts.setLanguage(Locale.US)
            val result = tts.setLanguage(Locale("ru"))
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
                setContentView(square_view)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CALL_PHONE),
                PERMISSIONS_REQUEST_CALL_PHONE
            )
        }
    }

    fun onHelp(view: View) {
        Toast.makeText(applicationContext, "Помощь", Toast.LENGTH_SHORT).show()
        //speak("Рассказываем, как пользоваться приложением")

        val intent = Intent(this, NavActivity::class.java)
        startActivity(intent)
    }

    fun notImplemented(view: View) {
        Toast.makeText(applicationContext, "Нет функции", Toast.LENGTH_SHORT).show()
        vibrator.createWaveFormVibrationUsingVibrationEffect()
    }

    fun debugLocation(view: View) {
        vibrator.createOneShotVibrationUsingVibrationEffect(1000) // test vibration
        locator.debugMode = !locator.debugMode
        Toast.makeText(applicationContext, "Locator debug is ${locator.debugMode}", Toast.LENGTH_SHORT).show()
    }

    fun onTest(view: View) {
        viewModel.requestAddress(HOME_LOCATION.longitude, HOME_LOCATION.latitude).observe(this) {
            if (it.success) {
                speak(it.short)
                Toast.makeText(applicationContext, "Address: ${it.long}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Error: ${it.error}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun goHome(view: View) {
        //speak("Строим маршрут для движения домой")
        val msg = locator.makePedestrianRoute(HOME_LOCATION,  {a -> onRouteResolve(a) } , {error -> onRouteResolveError(error)})
        speak(msg)
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    fun goHomeByTransport(view: View) {
        val msg = locator.makeTransportRoute(HOME_LOCATION, {a -> onRouteResolve(a) } , {error -> onRouteResolveError(error)})
        speak(msg)
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    fun goDrugstore(view: View) {
        //speak("Строим маршрут для движения в аптеку")
        val msg = locator.search("аптека", {a1, a2 -> onLocationResolve(a1, a2) } , {error -> onLocationError(error)})
        speak(msg)
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
    }

    fun goTransport(view: View) {
        val msg = locator.search("остановка автобуса", {a1, a2 -> onLocationResolve(a1, a2, true) } , {error -> onLocationError(error)})
        speak(msg)
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
    }

    fun callDoctor(view: View) {
        if (ynDialog != null) return
        val phone_number = "060"
        speak("""
            Вы собираетесь набрать номер $phone_number; 
            Чтобы подтвердтить, нажмите кнопку ДА сверху или кнопку громкости вверх.
            Чтобы отказаться, нажмите кнопку НЕТ снизу или кнопку громкости вниз.
            """)

        //val callDialog = YNDialogFragment()
        ynDialog = YNDialogFragment()
        ynDialog!!.onYes = {
            speak("звоню")
            //val phone_intent = Intent(Intent.ACTION_CALL)
            val phone_intent = Intent(Intent.ACTION_CALL)
            phone_intent.data = Uri.parse("tel:$phone_number")
            startActivity(phone_intent)
            ynDialog = null
        }
        ynDialog!!.onNo = {
            speak("отбой")
            ynDialog = null
        }
        ynDialog!!.show(supportFragmentManager)
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
            //GeocoderHelper.requestAddress1(pos.longitude, pos.latitude, {a1, a2 -> onLocationResolve(a1, a2) } , {error -> onLocationError(error)})
            viewModel.requestAddress(pos.longitude, pos.latitude).observe(this) {
                if (it.success) {
                    speak(it.short)
                    Toast.makeText(applicationContext, "Address: ${it.long}", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    speak("Ошибка при определении местоположения")
                    Toast.makeText(applicationContext, "Error: ${it.error}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        } else
            speak("Местоположение определяется, попробуйте повторить через несколько секунд")

    }

    fun onLocationResolve(address1: String, address2: String, full: Boolean = false) {
        if (full) speak(address1 + ", " + address2)
        else speak(address1)
        Toast.makeText(this@MainActivity, address1 + ", " + address2, Toast.LENGTH_LONG).show()
    }

    fun onLocationError(error: String) {
        speak(error)
        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
    }

    fun onRouteResolve(a: String) {
        speak(a + ". Хотите начать маршрут?")
        Toast.makeText(this@MainActivity, a, Toast.LENGTH_LONG).show()
        if (ynDialog != null) return
        ynDialog = YNDialogFragment()
        ynDialog!!.onYes = {
            speak("Начинаем движение по маршруту")
            ynDialog = null
            val intent = Intent(this, NavActivity::class.java)
            startActivity(intent)
        }
        ynDialog!!.onNo = {
            speak("отбой")
            ynDialog = null
        }
        ynDialog!!.show(supportFragmentManager)
    }

    fun onRouteResolveError(error: String) {
        speak(error)
        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
    }

    // Volume UP
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        super.onKeyUp(keyCode, event)
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if(System.currentTimeMillis() - lastPressedUp < VOL_UP_DELAY) {
                //Log.i("KEY", "VolUP")
                callDoctor(this.round_view)
                lastPressedUp = 0
            } else {
                // handle YES action
                if (ynDialog != null) {
                    ynDialog!!.dismiss()
                    ynDialog!!.onYes()
                    lastPressedUp = 0
                } else
                    lastPressedUp = System.currentTimeMillis() // wait next press
            }
            return true
        }
        return false
    }
    // Volume DN
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        super.onKeyDown(keyCode, event)
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // handle NO action
            if (ynDialog != null) {
                ynDialog!!.dismiss()
                ynDialog!!.onNo()
            }
            return true
        }
        return false
    }


}
