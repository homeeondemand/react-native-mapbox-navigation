package com.homee.mapboxnavigation

import android.graphics.BitmapFactory
import android.graphics.Color
import android.widget.LinearLayout
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.ThemedReactContext
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import android.graphics.drawable.Drawable
import com.mapbox.maps.plugin.compass.compass
import java.net.URL
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.mapbox.maps.*
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar

class MapboxNavigationView(private val context: ThemedReactContext, private val mCallerContext: ReactApplicationContext): LinearLayout(context.baseContext) {
    private var origin: Point? = null
    private var destination: Point? = null
    private var shouldSimulateRoute = false
    private var showsEndOfRouteFeedback = false
    private var mapToken: String? = null
    private var navigationToken: String? = null
    private var camera: ReadableMap? = null
    private var destinationMarker: Drawable? = null
    private var userLocatorMap: Drawable? = null
    private var userLocatorNavigation: Drawable? = null
    private var styleURL: String? = null
    private var transportMode: String = "bike"
    private var showUserLocation = false
    private var markers: ReadableArray? = null
    private var polylines: ReadableArray? = null

    private var mapboxMap: MapboxMap? = null
    private var mapView: MapView? = null
    private var mapboxNavView: MapboxNavigationNavView? = null

    private var isNavigation = false
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private var polylineAnnotation: PolylineAnnotation? = null
    private var pointAnnotation: PointAnnotation? = null
    private var pointAnnotationManager: PointAnnotationManager? = null

    companion object {
        var instance: MapboxNavigationView? = null
    }

    init {
        createMap()
        updateMap()
        instance = this
    }

    private fun createMap() {
        mCallerContext.runOnUiQueueThread {
            ResourceOptionsManager.getDefault(context.baseContext, mapToken!!)

            val mapboxMapView = MapboxNavigationMapView(context, this, id)
            mapView = mapboxMapView.initMap()

            mapView?.let { mapView ->
                mapboxMap = mapView.getMapboxMap()

                mapView.logo?.marginLeft = 3000.0F
                mapView.compass?.enabled = false
                mapView.attribution?.iconColor = Color.TRANSPARENT
                mapView.scalebar?.enabled = false

                val annotationApi = mapView.annotations

                polylineAnnotationManager = annotationApi?.createPolylineAnnotationManager(mapView)
                pointAnnotationManager = annotationApi?.createPointAnnotationManager(mapView)
            }

        }
    }

    private fun updateMap() {
        if (styleURL != null) {
            mapboxMap?.loadStyleUri(styleURL!!) {
                customizeMap()
            }
        } else {
            customizeMap()
        }
    }

    private fun customizeMap() {
        updateCamera()

        if (showUserLocation) {
            mapView?.location?.updateSettings {
                enabled = true
                pulsingEnabled = false
            }
        }

        if (userLocatorMap != null) {
            mapView?.location?.locationPuck = LocationPuck2D(
                topImage = userLocatorMap,
            )
        }

        addPolylines()
        addMarkers()
    }

    fun updateCamera() {
        if (camera != null) {
            val center = try {
                Point.fromLngLat(
                    camera!!.getArray("center")!!.getDouble(1),
                    camera!!.getArray("center")!!.getDouble(0)
                )
            } catch (e: Exception) {
                mapboxMap?.cameraState?.center
            }

            val zoom = try {
                camera!!.getDouble("zoom")
            } catch (e: Exception) {
                15.0
            }

            val cameraOptions = CameraOptions.Builder()
                .center(center)
                .zoom(zoom)
                .pitch(0.0)
                .build()

            mapboxMap?.setCamera(cameraOptions)
        }
    }

    fun startNavigation() {
        if (navigationToken != null
            && destination != null
            && mapView != null) {
            isNavigation = true

            mapboxNavView = MapboxNavigationNavView(context, navigationToken!!, id, mapView!!)
            mapboxNavView!!.initNavigation(userLocatorNavigation)
            mapboxNavView!!.shouldSimulateRoute = shouldSimulateRoute
            mapboxNavView!!.startNavigation(mapView!!, origin!!, destination!!, transportMode)
        }
    }

    fun stopNavigation() {
            if (isNavigation && mapboxNavView != null) {
                isNavigation = false

                mapboxNavView!!.stopNavigation(camera)
            }

    }

    private fun addPolylines() {
        if (mapView != null) {
            if (polylines != null && polylineAnnotationManager != null) {
                removePolylines()

                Handler(Looper.getMainLooper()).post {
                    val points = mutableListOf<Point>()

                    for (i in 0 until polylines!!.size()) {
                        val coordinates = mutableListOf<Point>()
                        val polylineInfo = polylines!!.getMap(i)
                        val polyline = polylineInfo!!.getArray("coordinates")
                        val color = polylineInfo!!.getString("color")
                        val opacity = if(polylineInfo!!.hasKey("opacity")) polylineInfo!!.getDouble("opacity") else 1.0

                        for (j in 0 until polyline!!.size()) {
                            val polylineArr = polyline!!.getArray(j)!!
                            val lat = polylineArr.getDouble(0)
                            val lng = polylineArr.getDouble(1)
                            val point = Point.fromLngLat(lng, lat)

                            coordinates.add(point)
                            points.add(point)
                        }

                        val polylineAnnotationOptions = PolylineAnnotationOptions()
                            .withPoints(coordinates)
                            .withLineColor(color ?: "#00AA8D")
                            .withLineWidth(5.0)
                            .withLineOpacity(opacity)
                        polylineAnnotation =
                            polylineAnnotationManager!!.create(polylineAnnotationOptions)

                    }

                    val newCameraOptions = mapboxMap!!.cameraForCoordinates(
                        points,
                        EdgeInsets(
                            if (camera!!.hasKey("offset") && camera!!.getBoolean("offset")) 62.0 else 42.0,
                            32.0,
                            if (camera!!.hasKey("offset") && camera!!.getBoolean("offset")) 168.0 else 32.0,
                            32.0
                        )
                    )
                    mapboxMap?.setCamera(newCameraOptions)
                }
            } else {
                removePolylines()
            }
        }
    }

    private fun removePolylines() {
        if (polylineAnnotation != null) {
            Handler(Looper.getMainLooper()).post {
                polylineAnnotationManager?.deleteAll()
                polylineAnnotation = null
            }
        }
    }

    private fun addMarkers() {
        if (mapView != null) {
            if (markers != null && markers!!.size() > 0) {
                doAsync {
                    val points = mutableListOf<Point>()

                    var i = 0
                    while (i < markers!!.size()) {
                        val marker = markers!!.getMap(i)

                        if (marker != null) {
                            val markerLatitude = marker.getDouble("latitude")!!
                            val markerLongitude = marker.getDouble("longitude")!!

                            val markerIcon = marker.getMap("image")!!
                            val markerUrl = markerIcon.getString("uri")
                            val inputStream = URL(markerUrl).openStream()
                            val icon = BitmapFactory.decodeStream(inputStream)
                            val point = Point.fromLngLat(markerLongitude, markerLatitude)
                            val pointAnnotationOptions: PointAnnotationOptions =
                                PointAnnotationOptions()
                                    .withPoint(point)
                                    .withIconImage(icon)

                            points.add(point)

                            pointAnnotation = pointAnnotationManager?.create(pointAnnotationOptions)
                        }

                        i++
                    }

                    val newCameraOptions = mapboxMap!!.cameraForCoordinates(
                        points,
                        EdgeInsets(
                            if (camera!!.hasKey("offset") && camera!!.getBoolean("offset")) 62.0 else 42.0,
                            32.0,
                            if (camera!!.hasKey("offset") && camera!!.getBoolean("offset")) 168.0 else 32.0,
                            32.0
                        ))
                    mapboxMap?.setCamera(newCameraOptions)
                }
            } else {
                if (pointAnnotation != null) {
                    Handler(Looper.getMainLooper()).post {
                        pointAnnotationManager?.deleteAll()
                        pointAnnotation = null
                    }
                }
            }
        }
    }

    private fun sendErrorToReact(error: String?) {
        val event = Arguments.createMap()
        event.putString("error", error)
        context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onError", event)
    }

    fun setOrigin(origin: Point?) {
        this.origin = origin
        updateMap()
    }

    fun setDestination(destination: Point?) {
        this.destination = destination
        updateMap()
    }

    fun setShouldSimulateRoute(shouldSimulateRoute: Boolean) {
        this.shouldSimulateRoute = shouldSimulateRoute
        updateMap()
    }

    fun setShowsEndOfRouteFeedback(showsEndOfRouteFeedback: Boolean) {
        this.showsEndOfRouteFeedback = showsEndOfRouteFeedback
        updateMap()
    }

    fun setMapToken(mapToken: String) {
        this.mapToken = mapToken
        updateMap()
    }

    fun setTransportMode(transportMode: String?) {
        if(transportMode != null) {
            this.transportMode = transportMode
            updateMap()
        }
    }

    fun setNavigationToken(navigationToken: String) {
        this.navigationToken = navigationToken
        updateMap()
    }

    fun setCamera(camera: ReadableMap) {
        this.camera = camera
        updateMap()
    }

    fun setDestinationMarker(destinationMarker: ReadableMap) {
        doAsync {
            val imageUrl = destinationMarker?.getString("uri")
            val inputStream = URL(imageUrl).openStream()
            val drawable = Drawable.createFromStream(inputStream, "src")
            this.destinationMarker = drawable
            updateMap()
        }
    }

    fun setUserLocatorMap(userLocatorMap: ReadableMap) {
        doAsync {
            val imageUrl = userLocatorMap?.getString("uri")
            val inputStream = URL(imageUrl).openStream()
            val drawable = Drawable.createFromStream(inputStream, "src")
            this.userLocatorMap = drawable
            updateMap()
        }
    }

    fun setUserLocatorNavigation(userLocatorNavigation: ReadableMap) {
        doAsync {
            val imageUrl = userLocatorNavigation?.getString("uri")
            val inputStream = URL(imageUrl).openStream()
            val drawable = Drawable.createFromStream(inputStream, "src")
            this.userLocatorNavigation = drawable
            updateMap()
        }
    }

    fun setStyleURL(styleURL: String) {
        this.styleURL = styleURL
        updateMap()
    }

    fun setShowUserLocation(showUserLocation: Boolean) {
        this.showUserLocation = showUserLocation
        updateMap()
    }

    fun setMarkers(markers: ReadableArray?) {
        this.markers = markers
        updateMap()
    }

    fun setPolylines(polylines: ReadableArray?) {
        this.polylines = polylines
        updateMap()
    }

    fun onDropViewInstance() {
        mapView?.onDestroy()
    }

    class doAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
        init {
            execute()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            handler()
            return null
        }
    }
}
