package com.example.myapplication

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.transport.masstransit.Route
import java.util.Locale


class NavActivity : AppCompatActivity() {
    private val TAG = "NAV"
    private val icons = arrayOf(R.drawable.baseline_arrow_upward, R.drawable.baseline_arrow_right,
        R.drawable.baseline_arrow_downward, R.drawable.baseline_arrow_left, )
    private val testText = arrayOf("10", "100", "500", "1000")
    private val announce = arrayOf("Вперед", "Направо", "Назад", "Налево")
    private lateinit var drawables: ArrayList<Drawable>
    private var dir = 0
    private  lateinit var locator: Locator
    private var route : Route? = null
    // add status: waitloc, onroute...

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutInflater.inflate(R.layout.activity_nav, null))
        drawables = icons.map { AppCompatResources.getDrawable(applicationContext, it) } as ArrayList<Drawable>
        locator = Locator.getInstance(this)
    }

    override fun onStart() {
        super.onStart()
        Log.i( TAG, "onStart")
    }

    override fun onStop() {
        super.onStop()
        Log.i( TAG, "onStop")
    }

    override fun onResume() {
        super.onResume()
        locator.subscribeToLocationUpdate(this::onLocationUpdate)
        route = locator.loadRoute(false)
        route?.let {
            val npoints = it.geometry.points.size // route points (we should use these for nav)
            val nwpoints = it.metadata.wayPoints.size // user-defined start, end, intermediate
            val nsects = it.sections.size
            findViewById<TextView>(R.id.routeView).text = "Route loaded $npoints, $nwpoints, $nsects"
        }
        Log.i( TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        locator.unsubscribeFromLocationUpdate()
        Log.i( TAG, "onPause")
    }

    private fun onLocationUpdate(location: Location?) {
        var msg = "Loc not avail"
        if (location != null) {
            val pos = location.position
            var speedStr = "0"
            if (location.speed != null) {
                //speedStr = String.format("%.1f", location.speed)
                speedStr = "%,.1f".format(Locale.ENGLISH, location.speed)
            }
            msg= "${pos.latitude},${pos.longitude} (${location.accuracy?.toInt()}) ${location.heading?.toInt()}, $speedStr"

            // if status waitloc -> to onroute, find nearest points, target to next
            // if close to next, switch to next.next
            // adjust bearing to next
        }
        findViewById<TextView>(R.id.statusView).text = msg
    }

    fun onTurn(view: View) {
        //Toast.makeText(applicationContext, "Turn", Toast.LENGTH_SHORT).show()
        dir = (dir + 1) % 4
        findViewById<ImageView>(R.id.imageView).setImageDrawable(drawables[dir])
        findViewById<TextView>(R.id.textView).text = testText[dir]
        TTS.speak(announce[dir])
        findViewById<TextView>(R.id.statusView).text = announce[dir]
    }
}

