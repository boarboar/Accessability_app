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
import java.lang.System.currentTimeMillis
import java.util.Locale
import kotlin.math.roundToInt


class NavActivity : AppCompatActivity() {
    private val TAG = "NAV"
    private val NCOURSE = 8
    private val ANNOUNCE_PROBLEM_PERIOD= 10_000L
    private val ANNOUNCE_MOVE_PERIOD= 5_000L

    private val icons = arrayOf(R.drawable.baseline_arrow_upward, R.drawable.baseline_north_east_24,
        R.drawable.baseline_arrow_right, R.drawable.baseline_south_east_24,
        R.drawable.baseline_arrow_downward, R.drawable.baseline_south_west_24,
        R.drawable.baseline_arrow_left, R.drawable.baseline_north_west_24, R.drawable.baseline_gps_not_fixed_24)
    private val announce = arrayOf("Вперед", "Правее", "Направо",
        "Назад", "Назад", "Назад",
        "Налево", "Левее")
    private lateinit var drawables: ArrayList<Drawable>
    private lateinit var routeInfo: TextView
    private lateinit var distanceInfo: TextView
    private lateinit var statusInfo: TextView
    private lateinit var directionInfo: ImageView
    private var test_Dir = 0
    private  lateinit var locator: Locator
    private var route : Route? = null
    private val navigator =  Navigator()
    private var prevAnnouncedResult = navigator.DummyResult
    private var prevAnnouncedResultTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutInflater.inflate(R.layout.activity_nav, null))
        drawables = icons.map { AppCompatResources.getDrawable(applicationContext, it) } as ArrayList<Drawable>
        locator = Locator.getInstance(this)
        routeInfo = findViewById<TextView>(R.id.routeView)
        distanceInfo = findViewById<TextView>(R.id.textView)
        statusInfo = findViewById<TextView>(R.id.statusView)
        directionInfo = findViewById<ImageView>(R.id.imageView)
    }

    override fun onResume() {
        super.onResume()
        locator.subscribeToLocationUpdate(this::onLocationUpdate)
        route = locator.loadRoute(false)
        route?.let {
            val npoints = it.geometry.points.size // route points (we should use these for nav)
            val nwpoints = it.metadata.wayPoints.size // user-defined start, end, intermediate
            val nsects = it.sections.size
            val dist = it.metadata.weight.walkingDistance.value
            //routeInfo.text = "Route loaded $npoints, $nwpoints, $nsects $dist m"
            routeInfo.text = "Маршрут: $npoints точек, $dist метров"
            // log 10 first points
            // it seems at least first two points are coincide
            Log.i( TAG, "Route loaded $npoints, $nwpoints, $nsects, $dist")
            if (it.geometry.points.size > 1) {
                var p0 = it.geometry.points[0]
                it.geometry.points.take(10).forEachIndexed() { i, p ->
                    Log.i( TAG, "$i ${(Geo::distance)(p, p0).toInt()} ${(Geo::course)(p, p0).toInt()}")
                    p0 = p
                }
            }
        }
        navigator.route = route
        directionInfo.setImageDrawable(drawables[drawables.size-1])
        distanceInfo.text = "---"
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
            val now = currentTimeMillis()
            var speedStr = "0"
            if (location.speed != null) {
                speedStr = "%,.1f".format(Locale.ENGLISH, location.speed)
            }
            statusInfo.text = "${pos.latitude},${pos.longitude} (${location.accuracy?.toInt()}) ${location.heading?.toInt()}, $speedStr"
            val res = navigator.update(location)

            routeInfo.text = res.debugStr

            when (res.type) {
                Navigator.Result.ResultType.Ignore, Navigator.Result.ResultType.Finished,-> {
                    return
                }
                Navigator.Result.ResultType.LowAccuracy, Navigator.Result.ResultType.LowSpeed,-> {
                    directionInfo.setImageDrawable(drawables[drawables.size-1])
                    if (res.type != prevAnnouncedResult.type || (now - prevAnnouncedResultTime) >= ANNOUNCE_PROBLEM_PERIOD) {
                        var message = if(res.type == Navigator.Result.ResultType.LowAccuracy) "Низкая точность" else "Идите быстрее"
                        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                        TTS.speak(message)
                        prevAnnouncedResult = res
                        prevAnnouncedResultTime = now
                    }
                    return
                }
                Navigator.Result.ResultType.Proceed -> {
                    var cdir = res.heading
                    val seg = 360f / NCOURSE
                    val dir =  (cdir / seg).roundToInt() % NCOURSE
                    val color =  if (res.status == Navigator.Status.LostRoute) R.color.red else {
                        if (res.backJump) R.color.blue
                        else if (location.accuracy!! > 5 ) R.color.yellow
                        else R.color.black
                    }
                    val dist = res.dist.toInt()
                    directionInfo.setColorFilter(ContextCompat.getColor(applicationContext, color), PorterDuff.Mode.SRC_IN);
                    directionInfo.setImageDrawable(drawables[dir])
                    if (res.backJump) {
                        distanceInfo.text = "$dist (<<<)"
                    }
                    else if (res.jump != null) {
                        distanceInfo.text = "$dist (${res.jump})"
                    }
                    else distanceInfo.text = dist.toString()

                    // announces
                    if (res.type != prevAnnouncedResult.type || res.status != prevAnnouncedResult.status
                        || (now - prevAnnouncedResultTime) >= ANNOUNCE_MOVE_PERIOD) {
                        var message = if(res.status == Navigator.Status.LostRoute) "Отклонились!" else ""
                        message += "${announce[dir]}, $dist метров"
                        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                        TTS.speak(message)
                        prevAnnouncedResult = res
                        prevAnnouncedResultTime = now
                    }
                }
                else -> {}
            }
        }
    }

    fun onAction(view: View) {
        /*
        // test
        val seg = 360 / NCOURSE
        test_Dir = (test_Dir + 1) % NCOURSE
        findViewById<ImageView>(R.id.imageView).setImageDrawable(drawables[test_Dir])
        findViewById<TextView>(R.id.textView).text = (test_Dir * seg).toString()
        TTS.speak(announce[test_Dir])
        findViewById<TextView>(R.id.statusView).text = announce[test_Dir]
    */
    }
}

