package com.example.myapplication

import android.app.Application
import android.util.Log
import com.yandex.mapkit.MapKitFactory


class MainApplication : Application() {
    /**
     * Replace "your_api_key" with a valid developer key.
     * You can get it at the https://developer.tech.yandex.ru/ website.
     */
    private val MAPKIT_API_KEY = "eec8cea9-5a04-41e5-b42b-09fb2eded6b2"
    override fun onCreate() {
        super.onCreate()
        // Set the api key before calling initialize on MapKitFactory.
        MapKitFactory.setApiKey(MAPKIT_API_KEY)
        MapKitFactory.setLocale("ru_RU")
        Log.i("MapKit", "Api key set")
    }
}
