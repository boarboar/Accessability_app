package com.example.myapplication

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class ReverseGeocodeResult(val display_name : String, val address : ReverseGeocodeAddress);

data class ReverseGeocodeAddress(val house_number : String, val building : String, val road : String,
                                 val municipality: String, val city : String, val state : String,
                                 val region: String, val postcode: String, val country: String, val amenity: String);


interface GeocoderApi {
    @GET("/reverse?lon=30.496880&lat=59.920199&accept-language=Ru_ru&format=json")
    suspend fun getReverseGeocodeTest() : Response<ReverseGeocodeResult>
    @GET("/reverse?accept-language=Ru_ru&format=json")
    suspend fun getReverseGeocode(@Query("lon") lon : Double, @Query("lat") lat : Double) : Response<ReverseGeocodeResult>
}

object GeocoderHelper {
    val baseUrl = "https://nominatim.openstreetmap.org"
    fun getInstance(): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            // we need to add converter factory to
            // convert JSON object to Java object
            .build()
    }
}
