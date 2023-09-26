package com.example.myapplication

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.yandex.mapkit.geometry.Geo
import com.yandex.mapkit.geometry.Segment
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.transport.masstransit.Route
import java.util.Locale


class NavActivity : AppCompatActivity() {
    enum class Status { NoRoute, Wait, OnRoute, Finished }
    private val TAG = "NAV"
    private val icons = arrayOf(R.drawable.baseline_arrow_upward, R.drawable.baseline_arrow_right,
        R.drawable.baseline_arrow_downward, R.drawable.baseline_arrow_left, )
    private val testText = arrayOf("10", "100", "500", "1000")
    private val announce = arrayOf("Вперед", "Направо", "Назад", "Налево")
    private lateinit var drawables: ArrayList<Drawable>
    private var dir = 0
    private  lateinit var locator: Locator
    private var route : Route? = null
    private var status = Status.Wait
    private val D_SNAP = 10f  // Close to route
    private val D_LOST = 15f  // Lost route
    private val D_TARG = 5f  // Arrival

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
        status = Status.NoRoute
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
                status = Status.Wait
                var p0 = it.geometry.points[0]
                it.geometry.points.take(10).forEachIndexed() { i, p ->
                    Log.i( TAG, "$i ${(Geo::distance)(p, p0).toInt()} ${(Geo::course)(p, p0).toInt()}")
                    p0 = p
                }
            }
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
            findViewById<TextView>(R.id.statusView).text = msg

            if (status == Status.NoRoute || status == Status.Finished) return

            route?.let {
                var points = it.geometry.points
                var cpi = 0
                var cdist =  (Geo::distance)(pos, points[0])
                points.forEachIndexed { i, p ->
                    val d = (Geo::distance)(pos, p)
                    if (d < cdist) {
                        cdist = d
                        cpi = i
                    }
                }

                var cseg = when (cpi) {
                    0 -> 0
                    points.size - 1 -> points.size - 2
                    else -> {
                        val dprev = (Geo::closestPoint)(pos, Segment(points[cpi-1], points[cpi]))
                        val dnext = (Geo::closestPoint)(pos, Segment(points[cpi], points[cpi+1]))
                        if ((Geo::distance)(pos, dprev) < (Geo::distance)(pos, dnext)) cpi-1
                        else cpi
                    }
                }
                val spoint = (Geo::closestPoint)(pos, Segment(points[cseg], points[cseg+1])) // closest point on seg
                val sdist = ((Geo::distance)(pos, spoint))// dist to seg
                val tpi = cseg + 1 // target
                val tpoint = points[tpi] //
                var tdist =  (Geo::distance)(pos, points[tpi])

                var text = "$cpi (${cdist.toInt()}) , $cseg (${cdist.toInt()}), $tpi (${tdist.toInt()}); ${(Geo::course)(pos, tpoint).toInt()}"
                var stat = ""

                when (status) {
                    Status.Wait -> {
                        if (cdist < D_SNAP) { //On route
                            stat = "ON ROUTE"
                            status = Status.OnRoute
                        } else {
                            stat = "OFF ROUTE"
                            // course: angle from NORTH to Vec(pos->p0)
                        }
                    }
                    Status.OnRoute -> {
                        if (cdist < D_LOST) { //On route
                            if (cpi == points.size - 1 && cdist <= D_TARG) {
                                status = Status.Finished
                                findViewById<TextView>(R.id.routeView).text = "FINISHED"
                                return
                            }
                            // follow...
                            stat = "FOLLOW"
                            // follow
                        } else {
                            stat = "LOST ROUTE"
                            // lost route...
                            status = Status.Wait
                        }
                    }
                    else -> {

                    }
                }
                findViewById<TextView>(R.id.routeView).text = text + " " + stat
                findViewById<TextView>(R.id.textView).text = tdist.toString()
            }
        }

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

