package com.homee.mapboxnavigation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
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
import com.mapbox.maps.plugin.logo.logo
import java.net.URL
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.mapbox.maps.*
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.attribution.attribution
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
    private var showUserLocation = false
    private var markers: ReadableArray? = null
    private var polyline: ReadableArray? = null

    private var mapboxMap: MapboxMap? = null
    private var mapView: MapView? = null

    private var isNavigation = false
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private var polylineAnnotation: PolylineAnnotation? = null
    private var pointAnnotation: PointAnnotation? = null
    private var pointAnnotationManager: PointAnnotationManager? = null

    init {
        mCallerContext.runOnUiQueueThread {
            if (navigationToken != null && destination != null) {
                isNavigation = true
                Log.w("MapboxNavigationView", " ----- init nav")
            } else {
                ResourceOptionsManager.getDefault(mCallerContext, mapToken)

                var mapboxMapView = MapboxNavigationMapView(context, this)

                mapView = mapboxMapView.mapView
            }

            mapView?.let { mapView ->
                mapboxMap = mapView.getMapboxMap()

                mapView.logo?.enabled = false
                mapView.compass?.enabled = false
                mapView.attribution?.iconColor = Color.TRANSPARENT
                mapView.scalebar?.enabled = false

                val annotationApi = mapView.annotations
                polylineAnnotationManager = annotationApi?.createPolylineAnnotationManager(mapView)
                pointAnnotationManager = annotationApi?.createPointAnnotationManager(mapView)
            }

            updateMap()
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
        if (camera != null) {
            val cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(
                    camera!!.getArray("center")!!.getDouble(0),
                    camera!!.getArray("center")!!.getDouble(1))
                )
                .zoom(camera!!.getDouble("zoom"))
                .pitch(0.0)
                .build()
            mapboxMap?.setCamera(cameraOptions)
        }

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

        if (isNavigation && userLocatorNavigation != null) {
            mapView?.location?.locationPuck = LocationPuck2D(
                topImage = userLocatorNavigation,
            )
        }

        addPolyline()
        addMarkers()
    }

    private fun addPolyline() {
        if (mapView != null) {
            if (polyline != null) {
                val points = mutableListOf<Point>()
                for (i in 0 until polyline!!.size()) {
                    val polylineArr = polyline!!.getArray(i)!!
                    val lat = polylineArr.getDouble(0)
                    val lng = polylineArr.getDouble(1)

                    points.add(Point.fromLngLat(lng, lat))
                }

                val polylineAnnotationOptions = PolylineAnnotationOptions()
                    .withPoints(points)
                    .withLineColor("#00AA8D")
                    .withLineWidth(5.0)

                polylineAnnotation = polylineAnnotationManager?.create(polylineAnnotationOptions)

                val newCameraOptions = mapboxMap!!.cameraForCoordinates(points, EdgeInsets(42.0, 32.0, 148.0 + 32.0, 32.0))
                mapboxMap?.setCamera(newCameraOptions)
            } else {
                Log.w("MapboxNavigationView", "will delete all polyline")
                if (polylineAnnotation != null) {
                    Log.w("MapboxNavigationView", "delete all polyline")
                    Handler(Looper.getMainLooper()).post {
                        polylineAnnotationManager?.onDestroy()
                        Log.w("MapboxNavigationView", "delete all polyline done")
                        polylineAnnotation = null
                    }
                }
            }
        }
    }

    private fun addMarkers() {
        if (mapView != null) {
            if (markers != null) {
                doAsync {
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
                            val pointAnnotationOptions: PointAnnotationOptions =
                                PointAnnotationOptions()
                                    .withPoint(
                                        Point.fromLngLat(
                                            markerLongitude,
                                            markerLatitude
                                        )
                                    )
                                    .withIconImage(icon)

                            pointAnnotation = pointAnnotationManager?.create(pointAnnotationOptions)
                        }

                        i++
                    }
                }
            } else {
                Log.w("MapboxNavigationView", "will delete all points")
                if (pointAnnotation != null) {
                    Log.w("MapboxNavigationView", "delete all points")
                    Handler(Looper.getMainLooper()).post {
                        pointAnnotationManager?.deleteAll()
                        Log.w("MapboxNavigationView", "delete all points done")
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

    //override fun onDestroy() {
    //    this.stopNavigation()
    //    this.mapboxNavigation?.onDestroy()
    //    super.onDestroy()
    //}

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
    
    fun setPolyline(polyline: ReadableArray?) {
        this.polyline = polyline
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