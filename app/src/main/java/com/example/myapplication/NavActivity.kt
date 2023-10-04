package com.example.myapplication

import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.yandex.mapkit.geometry.Geo
import com.yandex.mapkit.geometry.Segment
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.transport.masstransit.Route
import java.util.Locale
import kotlin.math.roundToInt


class NavActivity : AppCompatActivity() {
    private val TAG = "NAV"
    private val NCOURSE = 8
    private val icons = arrayOf(R.drawable.baseline_arrow_upward, R.drawable.baseline_north_east_24,
        R.drawable.baseline_arrow_right, R.drawable.baseline_south_east_24,
        R.drawable.baseline_arrow_downward, R.drawable.baseline_south_west_24,
        R.drawable.baseline_arrow_left, R.drawable.baseline_north_west_24, R.drawable.baseline_gps_not_fixed_24)
    //private val test_Text = arrayOf("10", "100", "500", "1000")
    private val announce = arrayOf("Вперед", "Правее", "Направо",
        "Назад", "Назад", "Назад",
        "Налево", "Левее")
    private lateinit var drawables: ArrayList<Drawable>
    private var test_Dir = 0
    private  lateinit var locator: Locator
    private var route : Route? = null
    private val navigator =  Navigator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutInflater.inflate(R.layout.activity_nav, null))
        drawables = icons.map { AppCompatResources.getDrawable(applicationContext, it) } as ArrayList<Drawable>
        locator = Locator.getInstance(this)
    }
    /*
    override fun onStart() {
        super.onStart()
        Log.i( TAG, "onStart")
    }

    override fun onStop() {
        super.onStop()
        Log.i( TAG, "onStop")
    }
   */

    override fun onResume() {
        super.onResume()
        locator.subscribeToLocationUpdate(this::onLocationUpdate)
        route = locator.loadRoute(false)
        route?.let {
            val npoints = it.geometry.points.size // route points (we should use these for nav)
            val nwpoints = it.metadata.wayPoints.size // user-defined start, end, intermediate
            val nsects = it.sections.size
            findViewById<TextView>(R.id.routeView).text = "Route loaded $npoints, $nwpoints, $nsects"
            // log 10 first points
            // it seems at least first two points are coincide
            Log.i( TAG, "Route loaded $npoints, $nwpoints, $nsects")
            if (it.geometry.points.size > 1) {
                //status = Status.Wait
                var p0 = it.geometry.points[0]
                it.geometry.points.take(10).forEachIndexed() { i, p ->
                    Log.i( TAG, "$i ${(Geo::distance)(p, p0).toInt()} ${(Geo::course)(p, p0).toInt()}")
                    p0 = p
                }
            }
        }
        navigator.route = route
        findViewById<ImageView>(R.id.imageView).setImageDrawable(drawables[drawables.size-1])
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
            findViewById<TextView>(R.id.statusView).text = msg
            val res = navigator.update(location)

            // check for dup stat here...
            Toast.makeText(applicationContext, res.type.toString(), Toast.LENGTH_SHORT).show()
            findViewById<TextView>(R.id.routeView).text = res.debugStr

            when (res.type) {
                Navigator.Result.ResultType.Ignore, Navigator.Result.ResultType.LowAccuracy,
                Navigator.Result.ResultType.LowSpeed, Navigator.Result.ResultType.Finished,-> {
                    findViewById<ImageView>(R.id.imageView).setImageDrawable(drawables[drawables.size-1])
                    return
                }
                Navigator.Result.ResultType.Proceed -> {
                    if (navigator.status == Navigator.Status.LostRoute) {
                        Toast.makeText(applicationContext, navigator.status.toString(), Toast.LENGTH_SHORT).show()
                    }
                    var cdir = res.heading
                    if (cdir < 0) {
                        cdir += 360
                    }
                    val seg = 360f / NCOURSE
                    val dir =  (cdir / seg).roundToInt() % NCOURSE
                    val color =  if (navigator.status == Navigator.Status.LostRoute) R.color.red else R.color.black
                    findViewById<ImageView>(R.id.imageView).setColorFilter(ContextCompat.getColor(applicationContext, color), PorterDuff.Mode.SRC_IN);
                    findViewById<ImageView>(R.id.imageView).setImageDrawable(drawables[dir])
                    findViewById<TextView>(R.id.textView).text = res.dist.toInt().toString()

                    // TTS.speak(announce[dir]) // + dist
                }
            }
        }
    }

    fun onTurn(view: View) {
        // test director
        val seg = 360 / NCOURSE
        test_Dir = (test_Dir + 1) % NCOURSE
        findViewById<ImageView>(R.id.imageView).setImageDrawable(drawables[test_Dir])
        findViewById<TextView>(R.id.textView).text = (test_Dir * seg).toString()
        TTS.speak(announce[test_Dir])
        findViewById<TextView>(R.id.statusView).text = announce[test_Dir]

    }
}

