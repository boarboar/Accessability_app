package com.example.myapplication

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.FilteringMode
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.Session.SearchListener
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.*
import com.yandex.mapkit.transport.masstransit.Session
import com.yandex.runtime.Error

class SearchListenerProxy(val onSuccess : (address1: String, address2: String) -> Unit, val onFailure : (error : String) -> Unit ) : SearchListener {
    private val TAG = "SLP"
    override fun onSearchResponse(p0: Response) {
        if(p0.collection.children.size > 0) {
            val data = p0.collection.children[0].obj
            data?.let {
                Log.w(TAG, it.name ?: "Address not available") // street
                Log.w(TAG, it.descriptionText ?: "Address1 not available") // city
                Log.w(TAG, "At: ${it.geometry[0].point?.latitude} ${it.geometry[0].point?.longitude}")
                //Toast.makeText(context, it!!.name ?: "Address not available", Toast.LENGTH_SHORT).show()
                if (it.name != null) {
                    onSuccess(it.name!!, it.descriptionText ?: "")
                } else {
                    onFailure("Нет имени")
                }
            }
            p0.collection.children.forEach {
                if (it.obj != null) {
                    Log.w("MapKit", ("Name:" + it.obj?.name))
                    Log.w("MapKit", ("Desc:" + it.obj?.descriptionText))
                }
            }
        } else {
            Log.e(TAG, "Empty result")
            onFailure("Поиск не вернул результатов")
        }
    }

    override fun onSearchError(p0: Error) {
        //Toast.makeText(context, "Ошибка поиска", Toast.LENGTH_SHORT).show()
        Log.e(TAG, p0.toString())
        onFailure("Ошибка поиска")
    }
}

class RouterProxy(val loc : Locator, val isTransport : Boolean, val onSuccess : (info: String) -> Unit, val onFailure : (error : String) -> Unit ) : Session.RouteListener {
    private val TAG = "SRP"
    override fun onMasstransitRoutes(p0: MutableList<Route>) {
        if (p0.size > 0) {
            var bestRoute = p0[0]
            val tlist = mutableListOf<String>()
            p0.forEach { route ->
                val points = route.geometry.points
                val metadata = route.metadata
                val from = metadata.wayPoints[0].position
                val to = metadata.wayPoints[metadata.wayPoints.size-1].position
                var isBest = false
                if (metadata.weight.walkingDistance.value < bestRoute.metadata.weight.walkingDistance.value) { // minimize walking
                    bestRoute = route
                    isBest = true
                    tlist.clear()
                }
                Log.i(TAG, "=== Route from: ${from.latitude} ${from.longitude} to: ${to.latitude} ${to.longitude}")
                Log.i(TAG, "Walking est: ${metadata.weight.walkingDistance.value} m., time: ${metadata.weight.time.value/60} m.")
                Log.i(TAG, "Route points: ${points.size} sections: ${route.sections.size}")
                route.sections.forEach { r ->
                    r.metadata.data.transports?.let { transports ->
                        if (transports.size > 0) {
                            val line = transports[0].line
                            val vehicle =
                                if (line.vehicleTypes.size > 0) line.vehicleTypes[0] else "?"
                            Log.i(
                                TAG,
                                "Section: ${line.id}, ${line.name}, ${line.transportSystemId}, ${line.shortName}, $vehicle"
                            )
                            /*
                            Log sample:
                            Section: 100000277, 2 линия, spb_metro, 2, underground
                            Section: 100000287, 3 линия, spb_metro, 3, underground
                            Section: 100000295, 4 линия, spb_metro, 4, underground
                            Section: bcbb_12_bus_discus, 12, null, null, bus
                            Vehicle types:
                            underground, bus, trolleybus, tramway
                            */
                            if (isBest)  tlist.add(when (vehicle) {
                                "underground" -> "Метро, линия ${line.shortName}"
                                "bus" -> "Автобус номер  ${line.name}"
                                "trolleybus" -> "Троллейбус номер ${line.name}"
                                "tramway" -> "Трамвай номер ${line.name}"
                                else -> vehicle
                            })
                        }
                    }
                }
            }

            //https://yandex.ru/dev/maps/mapkit/doc/android-ref/full/com/yandex/mapkit/directions/driving/DrivingRoute.html#getRoutePosition--
            val weight = bestRoute.metadata.weight
            loc.currentRoute = bestRoute
            loc.saveTransportRoute(bestRoute, isTransport)
            if (isTransport) {
                Log.i(
                    TAG,
                    "Best: ${tlist.joinToString(separator = ", ")}"
                )
                onSuccess(tlist.joinToString(separator = ", ") + "; Всего потребуется ${(weight.time.value/60).toInt()} минут, пешком потребуется пройти ${weight.walkingDistance.value} метров")
            }  else
                onSuccess("Всего потребуется ${(weight.time.value/60).toInt()} минут, пешком потребуется пройти ${weight.walkingDistance.value} метров")
        } else {
            Log.e(TAG, "Empty route")
            onFailure("Невозможно построить маршрут")
        }
    }

    override fun onMasstransitRoutesError(p0: Error) {
        //
        Log.e(TAG, p0.toString())
        onFailure("Ошибка построения маршрута")
    }

}

class Locator private constructor(private val context: AppCompatActivity) : LocationListener {
    companion object {
        @Volatile
        private var instance: Locator? = null
        /*
        fun init(context: AppCompatActivity) {
            instance = Locator(context)
        }
        */
        fun getInstance(context: AppCompatActivity): Locator {
            if (instance == null) {
                instance = Locator(context)
            }
            return instance!!
        }
    }

    private val PERMISSIONS_REQUEST_LOCATION = 1
    //private val DESIRED_ACCURACY = 5.0
    //private val MINIMAL_TIME: Long = 10000
    //private val MINIMAL_DISTANCE = 1.0
    private val DESIRED_ACCURACY = 0.0 // best
    private val MINIMAL_TIME = 2000L // 2sec
    //private val MINIMAL_DISTANCE = 1.5 // 1.5m
    private val MINIMAL_DISTANCE = 0.0 // try this to avoid stall
    private val USE_IN_BACKGROUND = false
    private val TRANSPORT_ROUTE_FILE = "SavedTransportRoute"
    private val PEDESTRIAN_ROUTE_FILE = "SavedPedestrianRoute"
    private val TAG = "LOC"

    private val DEFAULT_LOCATION = Point(59.972041, 30.323148) // test!!!

    init {
        MapKitFactory.initialize(context)
    }

    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private val locationManager = MapKitFactory.getInstance().createLocationManager()
    var location: Location? = null
    var debugMode = false
        set(value) {
            field = value
            if(value) {
                location = Location(DEFAULT_LOCATION, 30.0, null,null,null,null,0,0)
            }
        }
    init {
        requestLocationPermission()
        TransportFactory.initialize(context)
    }
    private val pedestrianRouter = TransportFactory.getInstance().createPedestrianRouter()
    private val masstransitRouter = TransportFactory.getInstance().createMasstransitRouter()
    private var onLocationUpdate : ((location: Location?) -> Unit)? = null
    var currentRoute : Route? = null


    override fun onLocationUpdated(location: Location) {
        if(debugMode) {
            return
        }
        val pos = location.position
        Log.w(TAG, "${pos.latitude},${pos.longitude} ( ${location.accuracy?.toInt()} ), ${location.heading}, ${location.speed}")
        this.location = location
        onLocationUpdate?.let {it(location)}
    }

    override fun onLocationStatusUpdated(locationStatus: LocationStatus) {
        if (locationStatus == LocationStatus.NOT_AVAILABLE) { Log.w("MapKit", "Loc not avail")
            //Toast.makeText(context, "Loc not avail", Toast.LENGTH_SHORT).show()
            onLocationUpdate?.let {it(null)}
        }
    }

    private fun requestLocationPermission() {
        // not clear which perm to require
        if (ContextCompat.checkSelfPermission(context,
                "android.permission.ACCESS_COARSE_LOCATION"
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context,
                "android.permission.ACCESS_FINE_LOCATION"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions( context,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    fun subscribeToLocationUpdate(listener: ((location: Location?) -> Unit)?) {
        MapKitFactory.getInstance().onStart()
        locationManager.subscribeForLocationUpdates(
            DESIRED_ACCURACY, MINIMAL_TIME, MINIMAL_DISTANCE,
            USE_IN_BACKGROUND, FilteringMode.OFF,
            this
        )
        onLocationUpdate = listener
        Log.i(TAG, "Subscribed to location update")
    }

    fun unsubscribeFromLocationUpdate() {
        locationManager.unsubscribe(this)
        MapKitFactory.getInstance().onStop()
        onLocationUpdate = null
        Log.i(TAG, "Unubscribed to location update")
    }

    /*
    fun requestAddress1(onSuccess : (address1: String, address2: String) -> Unit, onFailure : (error : String) -> Unit ) : String {
        if (location != null) {
            searchManager.submit(location!!.position, 18, SearchOptions().apply { searchTypes = SearchType.GEO.value }, SearchListenerProxy(onSuccess, onFailure) )
            return "Определяем адрес..."
        } else {
            return "Местоположение определяется, попробуйте повторить через несколько секунд"
        }
    }
    */

    fun search(query: String, onSuccess : (address1: String, address2: String) -> Unit, onFailure : (error : String) -> Unit ) : String {
        if (location != null) {
            searchManager.submit(query, Geometry.fromPoint(location!!.position), SearchOptions(), SearchListenerProxy(onSuccess, onFailure) )
            return "Определяем ближайшие объекты $query"
        } else {
            return "Местоположение определяется, попробуйте повторить через несколько секунд"
        }
    }

    fun makePedestrianRoute(to : Point, onSuccess : (address: String) -> Unit, onFailure : (error : String) -> Unit ) : String {
        if (location == null) {
            return "Местоположение определяется, попробуйте повторить через несколько секунд"
        }
        val points: MutableList<RequestPoint> = ArrayList()
        points.add(RequestPoint(location!!.position, RequestPointType.WAYPOINT, null))
        points.add(RequestPoint(to, RequestPointType.WAYPOINT, null))
        pedestrianRouter.requestRoutes(points, TimeOptions(), RouterProxy(this, false, onSuccess, onFailure))
        return "строим пеший маршрут"
    }

    fun makeTransportRoute(to : Point, onSuccess : (address: String) -> Unit, onFailure : (error : String) -> Unit) : String {
        if (location == null) {
            return "Местоположение определяется, попробуйте повторить через несколько секунд"
        }
        val points: MutableList<RequestPoint> = ArrayList()
        points.add(RequestPoint(location!!.position, RequestPointType.WAYPOINT, null))
        points.add(RequestPoint(to, RequestPointType.WAYPOINT, null))
        val options = TransitOptions(FilterVehicleTypes.NONE.value, TimeOptions())
        masstransitRouter.requestRoutes(points, options, RouterProxy(this, true, onSuccess, onFailure))
        return "строим транспортный маршрут"
    }

    fun saveTransportRoute(route: Route, isTransport: Boolean) {
        try {
            val filename = if (isTransport) TRANSPORT_ROUTE_FILE else PEDESTRIAN_ROUTE_FILE
            val bytes = if (isTransport) masstransitRouter.routeSerializer().save(route)
                    else pedestrianRouter.routeSerializer().save(route)
            context.openFileOutput(filename,
                Context.MODE_PRIVATE).use {
                it.write(bytes)
                Log.i(TAG, "Route saved")
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    fun loadRoute(isTransport: Boolean) : Route? {
        try {
            val filename = if (isTransport) TRANSPORT_ROUTE_FILE else PEDESTRIAN_ROUTE_FILE
            context.openFileInput(filename).use {
                val bytes = it.readBytes()
                if (bytes.isEmpty()) {
                    Log.w(TAG, "Saved route - empty array read")
                    return null
                }
                val route = if (isTransport)  masstransitRouter.routeSerializer().load(bytes)
                        else pedestrianRouter.routeSerializer().load(bytes)
                if (route == null) {
                    Log.w(TAG, "Saved route - failed to deserialize")
                    return null
                }
                val metadata = route.metadata
                val from = metadata.wayPoints[0].position
                val to = metadata.wayPoints[metadata.wayPoints.size-1].position
                Log.i(TAG, "=== Route loaded from: ${from.latitude} ${from.longitude} to: ${to.latitude} ${to.longitude}")

                return route
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
        return null
    }
}