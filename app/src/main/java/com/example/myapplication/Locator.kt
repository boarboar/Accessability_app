package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.location.*
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.Session.SearchListener
import com.yandex.runtime.Error

class SearchListenerProxy(val onSuccess : (address1: String, address2: String) -> Unit, val onFailure : (error : String) -> Unit ) : SearchListener {
    private val TAG = "SLP"
    override fun onSearchResponse(p0: Response) {
        if(p0.collection.children.size > 0) {
            val data = p0.collection.children[0].obj
            data?.let {
                Log.w(TAG, it.name ?: "Address not available") // street
                Log.w(TAG, it.descriptionText ?: "Address1 not available") // city
                //Toast.makeText(context, it!!.name ?: "Address not available", Toast.LENGTH_SHORT).show()
                if (it.name != null) {
                    onSuccess(it.name!!, it.descriptionText ?: "")
                } else {
                    onFailure("Нет имени")
                }
            }
            p0.collection.children.forEach {
                if (it.obj != null) {
                    Log.w("MapKit", "Name:" + it.obj!!.name ?: "")
                    Log.w("MapKit", "Desc:" + it.obj!!.descriptionText ?: "")
                }
            }
        } else {
            onFailure("Поиск не вернул результатов")
        }
    }

    override fun onSearchError(p0: Error) {
        //Toast.makeText(context, "Ошибка поиска", Toast.LENGTH_SHORT).show()
        Log.e(TAG, p0.toString())
        onFailure("Ошибка поиска")
    }
}

class Locator(val context: AppCompatActivity) : LocationListener {
    private val PERMISSIONS_REQUEST_FINE_LOCATION = 1
    private val DESIRED_ACCURACY = 5.0
    private val MINIMAL_TIME: Long = 10000
    private val MINIMAL_DISTANCE = 1.0
    private val USE_IN_BACKGROUND = false
    private val TAG = "LOC"

    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private val locationManager = MapKitFactory.getInstance().createLocationManager()
    var location: Location? = null
    //    private set

    init {
        requestLocationPermission()
    }

    /*
    val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("MapKit", "Location permission granted!")
            } else {
                Log.e("MapKit", "Location permission not granted!")
            }
        }
*/

    override fun onLocationUpdated(location: Location) {
        val pos = location.position
        val msg = "" + pos.getLatitude() + "," + pos.getLongitude() + " (" +
                location.accuracy?.toInt() + ")"
        Log.w(TAG, msg)
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        this.location = location
    }
    override fun onLocationStatusUpdated(locationStatus: LocationStatus) {
        if (locationStatus == LocationStatus.NOT_AVAILABLE) { Log.w("MapKit", "Loc not avail")
            Toast.makeText(context, "Loc not avail", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(context,
                "android.permission.ACCESS_FINE_LOCATION"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            /*
            requestPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) */
            ActivityCompat.requestPermissions(
                context, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_FINE_LOCATION
            )
        }
    }

    fun subscribeToLocationUpdate() {
        locationManager.subscribeForLocationUpdates(
            DESIRED_ACCURACY, MINIMAL_TIME, MINIMAL_DISTANCE,
            USE_IN_BACKGROUND, FilteringMode.OFF,
            this
        )
        Log.i(TAG, "Subscribed to location update")
    }

    fun unsubscribeFromLocationUpdate() {
        locationManager.unsubscribe(this)
    }

    fun requestAddress1(onSuccess : (address1: String, address2: String) -> Unit, onFailure : (error : String) -> Unit ) : String {
        if (location != null) {
            searchManager.submit(location!!.position, 18, SearchOptions().apply { searchTypes = SearchType.GEO.value }, SearchListenerProxy(onSuccess, onFailure) )
            return "Определяем адрес..."
        } else {
            return "Местоположение определяется, попробуйте повторить через несколько секунд"
        }
    }

    fun search(query: String, onSuccess : (address1: String, address2: String) -> Unit, onFailure : (error : String) -> Unit ) : String {
        if (location != null) {
            searchManager.submit(query, Geometry.fromPoint(location!!.position), SearchOptions(), SearchListenerProxy(onSuccess, onFailure) )
            return "Определяем ближайшие объекты $query"
        } else {
            return "Местоположение определяется, попробуйте повторить через несколько секунд"
        }
    }
}