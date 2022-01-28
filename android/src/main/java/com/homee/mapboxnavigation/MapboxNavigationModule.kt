package com.homee.mapboxnavigation

import com.facebook.react.bridge.*

class MapboxNavigationModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

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
    fun captureScreenshot(callBack: Callback) {
        MapboxNavigationView.instance?.captureScreenshot(callBack)
    }

    @ReactMethod
    fun setCamera(camera: ReadableMap) {
        if (MapboxNavigationView.instance != null && MapboxNavigationView.instance?.mapboxMap != null) {
            MapboxNavigationView.instance?.setCamera(camera)
        }
    }
}