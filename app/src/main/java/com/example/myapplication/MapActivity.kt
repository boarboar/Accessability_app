package com.example.myapplication

import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapKitFactory.initialize(this)
        setContentView(R.layout.activity_map)
        mapView = findViewById(R.id.mapview)
        mapView.mapWindow.map.move(
            CameraPosition(
                Point(59.920499, 30.497943),
                /* zoom = */ 13.0f,
                /* azimuth = */ 0.0f,
                /* tilt = */ 0.0f
            )
        )
        /*
        val placemark = mapView.mapWindow.map.mapObjects.addPlacemark().apply {
            geometry = Point(59.920499, 30.497943)
            setIcon(ImageProvider.fromResource(this@MapActivity, R.drawable.baseline_home_24))
            IconStyle().apply { anchor = PointF(0.5f, 1.0f) }
        }.apply {
            isDraggable = true
        }
        */

        val placemark = mapView.mapWindow.map.mapObjects.addPlacemark().apply {
            geometry = Point(59.920499, 30.497943)
            setIcon(ImageProvider.fromResource(this@MapActivity, R.drawable.baseline_home_24))
        }


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
