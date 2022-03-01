package com.homee.mapboxnavigation

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.facebook.react.bridge.*
import com.mapbox.maps.MapView
import java.io.ByteArrayOutputStream

class MapboxNavigationModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private var mapSnapshot: String = ""

    override fun getName(): String {
        return "MapboxNavigationManager"
    }

    @ReactMethod
    fun startTracking() {
        MapboxNavigationView.instance?.startTracking()
    }
    @ReactMethod
    fun stopTracking() {
        MapboxNavigationView.instance?.stopTracking()
    }

    @ReactMethod
    fun startNavigation() {
        MapboxNavigationView.instance?.startNavigation()
    }
    @ReactMethod
    fun stopNavigation() {
        MapboxNavigationView.instance?.stopNavigation()
    }

    @ReactMethod
    fun captureScreenshot(promise: Promise) {
        if (mapSnapshot != ""){
            promise.resolve(mapSnapshot)
        }
        MapboxNavigationView.instance?.mapView!!.snapshot(MapView.OnSnapshotReady { bitmap: Bitmap? ->
            Log.w("MapboxNav snap", bitmap.toString())
            if (bitmap != null) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val b64String =
                    "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT)

                Log.w("MapboxNav snap 2", bitmap.toString())
                mapSnapshot = b64String
                promise.resolve(mapSnapshot)
            }
        })
    }

    @ReactMethod
    fun clearScreenshot(promise: Promise) {
        mapSnapshot = ""
    }

    @ReactMethod
    fun setCamera(camera: ReadableMap) {
        if (MapboxNavigationView.instance != null && MapboxNavigationView.instance?.mapboxMap != null) {
            MapboxNavigationView.instance?.setCamera(camera)
        }
    }
}
