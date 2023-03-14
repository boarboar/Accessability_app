package com.example.myapplication

import android.util.Log
import android.widget.Toast
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.SearchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class ReverseGeocodeResult(val display_name : String, val address : ReverseGeocodeAddress);

data class ReverseGeocodeAddress(val house_number : String, val building : String, val road : String,
                                 val municipality: String, val city : String, val state : String,
                                 val region: String, val postcode: String, val country: String);


interface GeocoderApi {
    @GET("/reverse?lon=30.496880&lat=59.920199&accept-language=Ru_ru&format=json")
    suspend fun getReverseGeocodeTest() : Response<ReverseGeocodeResult>
    @GET("/reverse?accept-language=Ru_ru&format=json")
    suspend fun getReverseGeocode(@Query("lon") lon : Double, @Query("lat") lat : Double) : Response<ReverseGeocodeResult>
}

object GeocoderHelper {
    val geocodeApi: GeocoderApi by lazy() {
        Log.d("GEO", "GeocoderApi object created")
        getInstance().create(GeocoderApi::class.java)
    }

    val baseUrl = "https://nominatim.openstreetmap.org"
    fun getInstance(): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            // we need to add converter factory to
            // convert JSON object to Java object
            .build()
    }

    fun requestAddress1(longitude : Double, latitude : Double, onSuccess : (address1: String, address2: String) -> Unit, onFailure : (error : String) -> Unit ) {
        GlobalScope.launch {
            var addressString = ""
            var addressStringFull  = ""
            try {
                //val result = geocodeApi.getReverseGeocodeTest();
                val result = geocodeApi.getReverseGeocode(longitude, latitude)
                if (result != null && result.body() != null) {
                    // Checking the results
                    val address = result.body()!!.address
                    var house = address.house_number
                    if (house == null)
                        house = address.building
                    else
                        house = house.replace(" к", " корпус ") //"12 к2" -> "12 корп 2"
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
                    onSuccess(addressString, addressStringFull)
                } else {
                    onFailure("Невозможно определить адрес")
                }
            }
        }
    }
}
