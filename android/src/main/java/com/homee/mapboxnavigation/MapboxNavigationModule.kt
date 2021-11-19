package com.homee.mapboxnavigation

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap

class MapboxNavigationModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "MapboxNavigationManager"
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
    fun setCamera(camera: ReadableMap) {
        if (MapboxNavigationView.instance != null && MapboxNavigationView.instance?.mapboxMap != null) {
            MapboxNavigationView.instance?.setCamera(camera)
        }
    }

}