package com.homee.mapboxnavigation

import android.content.pm.PackageManager
import android.util.Log
import android.widget.LinearLayout
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.mapbox.geojson.Point
import javax.annotation.Nonnull

class MapboxNavigationManager(var mCallerContext: ReactApplicationContext) : SimpleViewManager<MapboxNavigationView>() {

    override fun getName(): String {
        return "MapboxNavigation"
    }

    public override fun createViewInstance(@Nonnull reactContext: ThemedReactContext): MapboxNavigationView {
        return MapboxNavigationView(reactContext, mCallerContext)
    }

    override fun onDropViewInstance(view: MapboxNavigationView) {
        view.onDropViewInstance()
        super.onDropViewInstance(view)
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Map<String, String>>? {
        return MapBuilder.of<String, Map<String, String>>(
                "onLocationChange", MapBuilder.of("registrationName", "onLocationChange"),
                "onError", MapBuilder.of("registrationName", "onError"),
                "onCancelNavigation", MapBuilder.of("registrationName", "onCancelNavigation"),
                "onArrive", MapBuilder.of("registrationName", "onArrive"),
                "onRouteProgressChange", MapBuilder.of("registrationName", "onRouteProgressChange"),
                "onNavigationStarted", MapBuilder.of("registrationName", "onNavigationStarted"),
        )
    }

    @ReactProp(name = "origin")
    fun setOrigin(view: MapboxNavigationView, sources: ReadableArray?) {
        if (sources == null || sources.toArrayList().filterNotNull().count() == 0) {
            view.setOrigin(null)
            return
        }
        view.setOrigin(Point.fromLngLat(sources.getDouble(0), sources.getDouble(1)))
    }

    @ReactProp(name = "destination")
    fun setDestination(view: MapboxNavigationView, sources: ReadableArray?) {
        if (sources == null || sources.toArrayList().filterNotNull().count() == 0) {
            view.setDestination(null)
            return
        }
        view.setDestination(Point.fromLngLat(sources.getDouble(0), sources.getDouble(1)))
    }

    @ReactProp(name = "shouldSimulateRoute")
    fun setShouldSimulateRoute(view: MapboxNavigationView, shouldSimulateRoute: Boolean) {
        view.setShouldSimulateRoute(shouldSimulateRoute)
    }

    @ReactProp(name = "showsEndOfRouteFeedback")
    fun setShowsEndOfRouteFeedback(view: MapboxNavigationView, showsEndOfRouteFeedback: Boolean) {
        view.setShowsEndOfRouteFeedback(showsEndOfRouteFeedback)
    }

    @ReactProp(name = "mapToken")
    fun setMapToken(view: MapboxNavigationView, mapToken: String) {
        view.setMapToken(mapToken)
    }

    @ReactProp(name = "navigationToken")
    fun setNavigationToken(view: MapboxNavigationView, navigationToken: String) {
        view.setNavigationToken(navigationToken)
    }

    @ReactProp(name = "camera")
    fun setCamera(view: MapboxNavigationView, camera: ReadableMap) {
        view.setCamera(camera)
    }

    @ReactProp(name = "destinationMarker")
    fun setDestinationMarker(view: MapboxNavigationView, destinationMarker: ReadableMap) {
        view.setDestinationMarker(destinationMarker)
    }

    @ReactProp(name = "userLocatorMap")
    fun setUserLocatorMap(view: MapboxNavigationView, userLocatorMap: ReadableMap) {
        view.setUserLocatorMap(userLocatorMap)
    }

    @ReactProp(name = "userLocatorNavigation")
    fun setUserLocatorNavigation(view: MapboxNavigationView, userLocatorNavigation: ReadableMap) {
        view.setUserLocatorNavigation(userLocatorNavigation)
    }

    @ReactProp(name = "styleURL")
    fun setStyleURL(view: MapboxNavigationView, styleURL: String) {
        view.setStyleURL(styleURL)
    }

    @ReactProp(name = "showUserLocation")
    fun setShowUserLocation(view: MapboxNavigationView, showUserLocation: Boolean) {
        view.setShowUserLocation(showUserLocation)
    }

    @ReactProp(name = "markers")
    fun setMarkers(view: MapboxNavigationView, markers: ReadableArray?) {
        view.setMarkers(markers)
    }
    
    @ReactProp(name = "polyline")
    fun setPolyline(view: MapboxNavigationView, polyline: ReadableArray?) {
        view.setPolyline(polyline)
    }

}