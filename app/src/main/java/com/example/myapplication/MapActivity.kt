package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var map: Map
    private lateinit var placemark: PlacemarkMapObject
    private  lateinit var locator: Locator

    private val inputListener = object : InputListener {
        override fun onMapLongTap(map: Map, point: Point) {
            // Move placemark after long tap
            placemark.geometry = point
        }

        override fun onMapTap(map: Map, point: Point) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locator = Locator.getInstance(this)

        MapKitFactory.initialize(this)
        setContentView(R.layout.activity_map)
        mapView = findViewById(R.id.mapview)
        map = mapView.mapWindow.map
        val home = locator.homeLocation
        map.move(
            CameraPosition(
                home,
                /* zoom = */ 13.0f,
                /* azimuth = */ 0.0f,
                /* tilt = */ 0.0f
            )
        )
        val imageProvider = ImageProvider.fromResource(this@MapActivity, R.drawable.ic_dollar_pin)
        /*
        placemark = mapView.mapWindow.map.mapObjects.addPlacemark().apply {
            geometry = Point(59.920499, 30.497943)
            setIcon(imageProvider)
            IconStyle().apply { anchor = PointF(0.5f, 1.0f) }
        }.apply {
            isDraggable = true
        }
        */

        placemark = map.mapObjects.addPlacemark().apply {
            geometry = home
            setIcon(imageProvider)
        }

        placemark.addTapListener { _, point ->
            Toast.makeText(
                this@MapActivity,
                "Tapped the point (${point.longitude}, ${point.latitude})",
                Toast.LENGTH_SHORT
            ).show()
            true
        }

        map.addInputListener(inputListener)
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

}
