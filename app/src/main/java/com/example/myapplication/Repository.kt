package com.example.myapplication

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.liveData

data class AddressResult(val short: String, val long: String, val success: Boolean, val error: String)

class Repository {
    val geocodeApi = GeocoderHelper.getInstance().create(GeocoderApi::class.java)

    suspend fun requestAddress(longitude : Double, latitude : Double) : AddressResult {
        var errmsg = ""
        try {
            val result =geocodeApi.getReverseGeocode(longitude, latitude)
            if (result != null && result.body() != null) {
                // Checking the results
                val address = result.body()!!.address
                var house = address.house_number
                if (house != null)
                    house = house.replace(" к", " корпус ") //"12 к2" -> "12 корп 2"
                else {
                    house = address.building
                    if (house == null)
                        house = address.amenity
                    if (house == null)
                        house = ""
                }
                Log.d("GEO", result.body()!!.display_name)
                Log.d("GEO", address.toString())
                return AddressResult("${address.road} ${house}", result.body()!!.display_name, true, "")
            } else {
                Log.e("GEO", "Failed with $result")
                errmsg = result.body().toString()
            }
        } catch (e: Throwable) {
            Log.e("GEO", e.toString())
            errmsg = e.message ?: "Неизвестная ошибка"
        }
        return AddressResult("", "", false, errmsg)
    }
}

class MainViewModel : ViewModel() {
    val repository: Repository = Repository()
    /*
    val requestAddressTest = liveData(Dispatchers.IO) {
        val retrivedAddress = repository.getReverseGeocodeTest()
        emit(retrivedAddress)
    }
*/
    fun requestAddress(longitude : Double, latitude : Double) =
        liveData(Dispatchers.IO) {
            val retrievedAddress = repository.requestAddress(longitude, latitude)
            emit(retrievedAddress)
        }


}