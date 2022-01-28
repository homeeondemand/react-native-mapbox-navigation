package com.homee.mapboxnavigation

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.widget.LinearLayout
import com.facebook.react.bridge.*
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap
import java.io.ByteArrayOutputStream
import java.lang.RuntimeException
import java.net.URL


@SuppressLint("ViewConstructor")
class MapboxNavigationView(private val context: ThemedReactContext, private val mCallerContext: ReactApplicationContext): LinearLayout(context.baseContext) {
    private val annotationLayerId = "mapbox-android-polylineAnnotation-layer-1"
    private var annotationLayerDisplayed = false
    private var origin: Point? = null
    private var destination: Point? = null
    private var shouldSimulateRoute = false
    private var useImperial = false
    private var showsEndOfRouteFeedback = false
    private var mapToken: String? = null
    private var navigationToken: String? = null
    private var camera: ReadableMap? = null
    private var destinationMarker: Drawable? = null
    private var userLocatorMap: Drawable? = null
    private var userLocatorNavigation: Drawable? = null
    private var styleURL: String? = null
    private var transportMode: String = "bike"
    private var language: String = ""
    private var showUserLocation = false
    private var voiceEnabled = false
    private var markers: ReadableArray? = null
    private var polylines: ReadableArray? = null
    private var smallRender = false

    var mapboxMap: MapboxMap? = null
    private var mapView: MapView? = null
    private var mapboxNavigation: MapboxNavigationNavigation? = null

    private var isNavigation = false
    private var followUser = false
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private var polylineAnnotation: PolylineAnnotation? = null
    private var pointAnnotation: PointAnnotation? = null
    private var pointAnnotationManager: PointAnnotationManager? = null

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        if (followUser) {
            val cameraOptions = CameraOptions.Builder()
                .center(point)
                .build()

            mapboxMap?.setCamera(cameraOptions)
        }

    }
    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener { bearing ->
        if (isNavigation && followUser) {
            val cameraOptions = CameraOptions.Builder()
                .bearing(bearing)
                .build()

            mapboxMap?.setCamera(cameraOptions)
        }
    }
    private val onMoveListener = object : OnMoveListener {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }
        override fun onMoveBegin(detector: MoveGestureDetector) {
            sendEvent("onMapMove", Arguments.createMap())
        }
        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    companion object {
        var instance: MapboxNavigationView? = null
    }

    init {
        instance = this
    }

    @SuppressLint("ClickableViewAccessibility")
    fun createMap() {
        if (mapView != null) return

        ResourceOptionsManager.getDefault(context.baseContext, mapToken!!)

        val layout = inflate(context, R.layout.mapview_layout, this)

        mapView = layout.findViewById(R.id.mapView)

        mapView?.setOnTouchListener { _, motionEvent ->
            if(motionEvent.action == MotionEvent.ACTION_UP) {
                val event = Arguments.createMap()
                event.putString("onTap", "")
                context.getJSModule(RCTEventEmitter::class.java).receiveEvent(this.id, "onTap", event)
            }
            false
        }

        mapView?.let { mapView ->
            mapboxMap = mapView.getMapboxMap()
            mapView.logo.marginLeft = 3000.0F
            mapView.compass.enabled = false
            mapView.attribution.iconColor = Color.TRANSPARENT
            mapView.scalebar.enabled = false

            mapboxMap?.addOnMoveListener(onMoveListener)
            mapView.location.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
            mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)

            val annotationApi = mapView.annotations

            polylineAnnotationManager = annotationApi.createPolylineAnnotationManager(mapView)
            pointAnnotationManager = annotationApi.createPointAnnotationManager(mapView)
        }

        updateMap()
    }

    private fun updateMap() {
        Handler(Looper.getMainLooper()).post {
            if (!this.isNavigation) {
                fitCameraForAnnotations()
            }
            applyStyle()

            addMarkers()
            addPolylines()
        }
    }

    private fun applyStyle() {
        if (styleURL != null) {
            mapboxMap?.loadStyleUri(styleURL!!, Style.OnStyleLoaded {
                Log.i("MapboxNavigation", " Map style loaded")
                annotationLayerDisplayed = false
                for( layer in it.styleLayers) {
                    if(layer.id == annotationLayerId) {
                        annotationLayerDisplayed = true
                    }
                }
            }, onMapLoadErrorListener = object : OnMapLoadErrorListener {
                override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
                    Log.e("MapboxNavigation", eventData.message)
                }
            })
        }
    }

    private fun addPolylines() {
        if (mapView != null) {
            deletePolylines()
            if (polylines != null && polylineAnnotationManager != null && polylines!!.size() > 0) {
                for (i in 0 until polylines!!.size()) {
                    val coordinates = mutableListOf<Point>()
                    val polylineInfo = polylines!!.getMap(i)!!
                    val polyline = polylineInfo.getArray("coordinates")
                    val color = polylineInfo.getString("color")
                    val opacity =
                        if (polylineInfo.hasKey("opacity")) polylineInfo.getDouble("opacity") else 1.0

                    if (polyline!!.size() < 2) {
                        continue
                    }

                    for (j in 0 until polyline!!.size()) {
                        val polylineArr = polyline.getArray(j)!!
                        val lat = polylineArr.getDouble(0)
                        val lng = polylineArr.getDouble(1)
                        val point = Point.fromLngLat(lng, lat)

                        coordinates.add(point)
                    }

                    val polylineAnnotationOptions = PolylineAnnotationOptions()
                        .withPoints(coordinates)
                        .withLineColor(color ?: "#00AA8D")
                        .withLineWidth(5.0)
                        .withLineOpacity(opacity)
                    polylineAnnotation =
                        polylineAnnotationManager!!.create(polylineAnnotationOptions)
                }
            }
        }
    }

    private fun addMarkers() {
        if (mapView != null) {
            if (markers != null && markers!!.size() > 0) {
                DoAsync {
                    for (i in 0 until markers!!.size()) {
                        val marker = markers!!.getMap(i)!!

                        if (!marker.hasKey("latitude") || !marker.hasKey("longitude")) continue

                        val markerLatitude = marker.getDouble("latitude")
                        val markerLongitude = marker.getDouble("longitude")

                        val markerIcon = marker.getMap("image")!!
                        val markerUrl = markerIcon.getString("uri") ?: return@DoAsync
                        val icon = getDrawableFromUri(markerUrl)
                        val point = Point.fromLngLat(markerLongitude, markerLatitude)
                        val pointAnnotationOptions: PointAnnotationOptions =
                            PointAnnotationOptions()
                                .withPoint(point)

                        if (icon !== null) {
                            pointAnnotationOptions.withIconImage(icon.getBitmap())
                        }

                        pointAnnotationManager = mapView!!.annotations.createPointAnnotationManager(mapView!!)
                        pointAnnotation = pointAnnotationManager?.create(pointAnnotationOptions)
                    }
                }
            } else {
                if (pointAnnotation != null) {
                    pointAnnotationManager?.deleteAll()
                    pointAnnotation = null
                }
            }
        }
    }

    private fun fitCameraForAnnotations() {
        val points = mutableListOf<Point>()

        // add polylines points
        if (polylines != null) {
            for (i in 0 until polylines!!.size()) {
                val polylineInfo = polylines!!.getMap(i)!!
                val polyline = polylineInfo.getArray("coordinates")

                for (j in 0 until polyline!!.size()) {
                    val polylineArr = polyline.getArray(j)!!
                    val lat = polylineArr.getDouble(0)
                    val lng = polylineArr.getDouble(1)
                    val point = Point.fromLngLat(lng, lat)

                    points.add(point)
                }
            }
        }

        // add markers points
        if (markers != null && markers!!.size() > 0) {
            for (i in 0 until markers!!.size()) {
                val marker = markers!!.getMap(i)!!

                if (!marker.hasKey("latitude") || !marker.hasKey("longitude")) continue

                val markerLatitude = marker.getDouble("latitude")
                val markerLongitude = marker.getDouble("longitude")
                val point = Point.fromLngLat(markerLongitude, markerLatitude)

                points.add(point)
            }
        }

        if (points.size > 0) {
            val mapWidth = mapView!!.width
            val mapHeight = mapView!!.height
            val newCameraOptions = mapboxMap!!.cameraForCoordinates(
                points,
                EdgeInsets(
                    mapHeight * 0.20 + (if (camera!!.hasKey("offset") && camera!!.getBoolean("offset")) 20 else 0),
                        mapWidth * 0.1,
                        mapHeight * 0.20 + (if (camera!!.hasKey("offset") && camera!!.getBoolean("offset")) 136 else 0),
                        mapWidth * 0.1,
                )
            )

            mapboxMap?.setCamera(newCameraOptions)
        } else {
            updateCamera()
        }
    }

    private fun updateCamera() {
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

            val pitch = try {
                camera!!.getDouble("pitch")
            } catch (e: Exception) {
                0.0
            }

            val cameraOptions = CameraOptions.Builder()
                .center(center)
                .zoom(zoom)
                .pitch(pitch)
                .build()

            mapboxMap?.setCamera(cameraOptions)
        }
    }

    private fun sendEvent(name: String, data: WritableMap) {
        context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, name, data)
    }

    private fun deletePolylines() {
        if (polylineAnnotation != null) {
            polylineAnnotationManager?.deleteAll()
            polylineAnnotation = null
        }
    }

    fun captureScreenshot(callBack: Callback) {
        if(mapView != null) {
            mapView!!.snapshot(MapView.OnSnapshotReady { bitmap: Bitmap? ->
                if(bitmap != null){
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    val byteArray = byteArrayOutputStream.toByteArray()
                    val b64String = "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT)

                    callBack(b64String)
                }
            })
        }
    }

    fun startNavigation() {
        if (navigationToken != null
            && destination != null
            && mapView != null
            && origin != null
        ) {
            isNavigation = true
            deletePolylines()
            setFollowUser(false)
            Handler(Looper.getMainLooper()).post {
                mapboxNavigation =
                    MapboxNavigationNavigation(context, navigationToken!!, id, mapView!!)
                mapboxNavigation?.startNavigation(
                    origin!!,
                    destination!!,
                    transportMode,
                    shouldSimulateRoute,
                    useImperial,
                    language,
                    voiceEnabled,
                    userLocatorNavigation,
                )
            }
        }
    }

    fun stopNavigation() {
        if (mapboxNavigation != null) {
            isNavigation = false
            setFollowUser(true)

            mapboxNavigation!!.stopNavigation()
        }
    }

    fun startTracking() {
        isNavigation = true
        setFollowUser(true)
    }

    fun stopTracking() {
        isNavigation = false

        mapView?.let {
            val cameraOptions = CameraOptions.Builder()
                .pitch(0.0)
                .bearing(null)
                .build()

            mapboxMap?.setCamera(cameraOptions)
        }
    }

    fun setOrigin(origin: Point?) {
        this.origin = origin
    }

    fun setDestination(destination: Point?) {
        this.destination = destination
    }

    fun setFollowUser(followUser: Boolean) {
        this.followUser = followUser

        if(mapboxNavigation != null) {
            mapboxNavigation?.followUser = followUser
        }
    }

    fun setUseImperial(useImperial: Boolean) {
        this.useImperial = useImperial
    }

    fun setVoiceEnabled(voiceEnabled: Boolean) {
        this.voiceEnabled = voiceEnabled
    }

    fun setShouldSimulateRoute(shouldSimulateRoute: Boolean) {
        this.shouldSimulateRoute = shouldSimulateRoute
    }

    fun setShowsEndOfRouteFeedback(showsEndOfRouteFeedback: Boolean) {
        this.showsEndOfRouteFeedback = showsEndOfRouteFeedback
    }

    fun setLanguage(language: String) {
        this.language = language
    }

    fun setMapToken(mapToken: String) {
        val needCreation = this.mapToken == null
        this.mapToken = mapToken

        if (needCreation) {
            createMap()
        }
    }

    fun setTransportMode(transportMode: String?) {
        if(transportMode != null) {
            this.transportMode = transportMode
        }
    }

    fun setNavigationToken(navigationToken: String) {
        this.navigationToken = navigationToken
    }

    fun setCamera(camera: ReadableMap) {
        val offset = if(camera.hasKey("offset"))
            camera.getBoolean("offset")
        else
            if(this.camera != null && this.camera!!.hasKey("offset"))
                this.camera!!.getBoolean("offset")
            else
                false
        val center = if(camera.hasKey("center"))
            camera.getArray("center")
        else
            if(this.camera != null && this.camera!!.hasKey("center"))
                this.camera!!.getArray("center")
            else
                null
        val zoom = if(camera.hasKey("zoom"))
            camera.getDouble("zoom")
        else
            if(this.camera != null && this.camera!!.hasKey("zoom"))
                this.camera!!.getDouble("zoom")
            else
                null
        val pitch = if(camera.hasKey("pitch"))
            camera.getDouble("pitch")
        else
            if(this.camera != null && this.camera!!.hasKey("pitch"))
                this.camera!!.getDouble("pitch")
            else
                null

        val newCamera = Arguments.createMap()
        if (center != null) {
            val centerWritableArray = Arguments.createArray()
            centerWritableArray.pushDouble(center.getDouble(0))
            centerWritableArray.pushDouble(center.getDouble(1))
            newCamera.putArray("center", centerWritableArray)
        }
        if (zoom != null) newCamera.putDouble("zoom", zoom)
        if (pitch != null) newCamera.putDouble("pitch", pitch)
        newCamera.putBoolean("offset", offset)

        this.camera = newCamera

        updateCamera()
    }

    fun setDestinationMarker(destinationMarker: ReadableMap) {
        DoAsync {
            val imageUrl = destinationMarker.getString("uri")
            val drawable: Drawable? = getDrawableFromUri(imageUrl)
            this.destinationMarker = drawable
            updateMap()
        }
    }

    fun setUserLocatorMap(userLocatorMap: ReadableMap) {
        DoAsync {
            val imageUrl = userLocatorMap.getString("uri")
            val drawable: Drawable? = getDrawableFromUri(imageUrl)
            this.userLocatorMap = drawable
            updateMap()
        }
    }

    fun setUserLocatorNavigation(userLocatorNavigation: ReadableMap) {
        DoAsync {
            val imageUrl = userLocatorNavigation.getString("uri")
            val drawable: Drawable? = getDrawableFromUri(imageUrl)
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
        mapView?.location?.updateSettings {
            pulsingEnabled = false
            if (showUserLocation) {
                enabled = true

                if (!isNavigation && annotationLayerDisplayed) {
                    layerAbove = annotationLayerId
                }

                if (userLocatorMap != null) {
                    locationPuck = LocationPuck2D(
                            topImage = userLocatorMap,
                    )
                }
            } else {
                enabled = false
            }
        }
    }

    fun setMarkers(markers: ReadableArray?) {
        this.markers = markers
    }

    fun setPolylines(polylines: ReadableArray?) {
        this.polylines = polylines
        updateMap()
    }

    fun setSmallRender(smallRender: Boolean = false) {
        this.smallRender = smallRender
    }

    fun onDropViewInstance() {
        mapView?.onDestroy()
    }

    private fun scaleDrawable(drawable: Drawable): Drawable {
        val bitmap = (drawable as BitmapDrawable).bitmap
        val ratio = if(smallRender) 80 else 100

        return BitmapDrawable(resources, Bitmap.createScaledBitmap(bitmap, ratio, ratio, true))
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getDrawableFromUri(imageUrl: String?): Drawable? {
        val drawable = if (imageUrl?.contains("http") == true) {
            val inputStream = URL(imageUrl).openStream()
            scaleDrawable(Drawable.createFromStream(inputStream, "src"))
        } else {
            val resourceId = mCallerContext.resources.getIdentifier(
                imageUrl,
                "drawable",
                mCallerContext.packageName
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                scaleDrawable(resources.getDrawable(resourceId, mCallerContext.theme))
            } else {
                TODO("VERSION.SDK_INT < LOLLIPOP")
            }
        }

        return drawable
    }

    @SuppressLint("NewApi")
    class DoAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
        init {
            execute()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            handler()
            return null
        }
    }
}
