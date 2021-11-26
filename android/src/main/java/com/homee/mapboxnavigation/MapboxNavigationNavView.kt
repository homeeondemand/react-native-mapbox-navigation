package com.homee.mapboxnavigation

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.*

class MapboxNavigationNavView(private val context: ThemedReactContext, private val token: String, private val id: Int, private val mapView: MapView) {
    private var mapboxMap: MapboxMap? = null
    var mapboxNavigation: MapboxNavigation? = null
    var shouldSimulateRoute: Boolean = false

    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private lateinit var navigationCamera: NavigationCamera
    private val routeLineResources: RouteLineResources by lazy {
        val routeLineColorResources = RouteLineColorResources
            .Builder()
            .routeDefaultColor(Color.parseColor("#00AA8D"))
            .routeCasingColor(Color.parseColor("#00AA8D"))
            .build()

        RouteLineResources.Builder()
            .routeLineColorResources(routeLineColorResources)
            .build()
    }
    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(context.baseContext)
            .withVanishingRouteLineEnabled(true)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label")
            .build()
    }
    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }
    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }
    private val locationObserver = object : LocationObserver {
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            val keyPoints = locationMatcherResult.keyPoints

            navigationLocationProvider.changePosition(
                enhancedLocation,
                keyPoints,
            )

            updateCamera(
                Point.fromLngLat(
                    enhancedLocation.longitude,
                    enhancedLocation.latitude
                ),
                enhancedLocation.bearing.toDouble()
            )
        }

        override fun onNewRawLocation(rawLocation: Location) {}
    }
    private val formatterOptions: DistanceFormatterOptions by lazy {
        DistanceFormatterOptions.Builder(context.baseContext).build()
    }
    private val maneuverApi: MapboxManeuverApi by lazy {
        MapboxManeuverApi(MapboxDistanceFormatter(formatterOptions))
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        val routeLines = listOf(RouteLine(routeProgress.route, null))

        mapboxMap?.getStyle()?.apply {
            routeLineApi.updateWithRouteProgress(routeProgress) { result ->
                routeLineView.renderRouteLineUpdate(this, result)
            }

            routeLineApi.setRoutes(routeLines) { value ->
                routeLineView.renderRouteDrawData(this, value)
            }
        }

        // RouteArrow: The next maneuver arrows are driven by route progress events.
        // Generate the next maneuver arrow update data and pass it to the view class
        // to visualize the updates on the map.
        //val arrowUpdate = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
        //mapboxMap?.getStyle()?.apply {
        //    // Render the result to update the map.
        //    routeArrowView.renderManeuverUpdate(this, arrowUpdate)
        //}

        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        val maneuvers = Arguments.createArray()
        for(leg in routeProgress.route.legs()!!) {
            for(step in leg.steps()!!) {
                val formattedManeuver = Arguments.createMap()

                formattedManeuver.putDouble("distance", step.distance())
                formattedManeuver.putString("turn", step.maneuver().modifier())
                formattedManeuver.putString("type", step.maneuver().type())
                formattedManeuver.putString("exitNumber", step.maneuver().exit().toString())
                formattedManeuver.putString("roadName", step.name())
                formattedManeuver.putString("instruction", step.maneuver().instruction())

                maneuvers.pushMap(formattedManeuver)
            }
        }

        val event = Arguments.createMap()

        event.putDouble("stepDistanceRemaining",
            routeProgress.currentLegProgress?.currentStepProgress?.distanceRemaining?.toDouble() ?: 0.0
        )
        event.putDouble("distanceTraveled", routeProgress.distanceTraveled.toDouble())
        event.putDouble("durationRemaining", routeProgress.durationRemaining)
        event.putDouble("fractionTraveled", routeProgress.fractionTraveled.toDouble())
        event.putDouble("distanceRemaining", routeProgress.distanceRemaining.toDouble())
        event.putDouble("eta", ((System.currentTimeMillis() / 1000) + routeProgress.durationRemaining))
        event.putArray("maneuvers", maneuvers)
        event.putString("route", routeLines[0].route.geometry())
        event.putInt("stepIndex", routeProgress.currentLegProgress?.currentStepProgress?.stepIndex ?: 0)

        sendEvent("onRouteProgressChange", event)
    }
    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        mapboxMap?.getStyle()?.apply {
            // Render the result to update the map.
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    private val onRerouteObserver = RerouteController.RerouteStateObserver { rerouteState ->
        val status = when (rerouteState::class.simpleName) {
            "FetchingRoute" -> "reroute"
            "RouteFetched" -> "done"
            else -> ""
        }
        val event = Arguments.createMap()
        event.putString("message", status)
        sendEvent("onReroute", event)
    }

    private val navigationLocationProvider = NavigationLocationProvider()
    private val mapboxReplayer = MapboxReplayer()
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
    private val locationComponent by lazy {
        mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
    }

    fun initNavigation (userLocatorNavigation: Drawable?): MapboxNavigation? {
        mapView.location.apply {
            setLocationProvider(navigationLocationProvider)

            if (userLocatorNavigation != null) {
                locationPuck = LocationPuck2D(
                    bearingImage = userLocatorNavigation,
                )
            }
            enabled = true
        }

        mapboxNavigation = if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve()
        } else {
            val navigationOptions = NavigationOptions.Builder(context.baseContext)
                .accessToken(token)
                .locationEngine(replayLocationEngine)
                .build()

            MapboxNavigationProvider.create(navigationOptions)
        }

        return mapboxNavigation
    }

    @SuppressLint("MissingPermission")
    fun startNavigation(mapView: MapView, origin: Point, destination: Point, transportMode: String) {
        mapboxMap = mapView.getMapboxMap()
        mapboxMap?.let {
            viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap!!)
            navigationCamera = NavigationCamera(
                mapboxMap!!,
                mapView.camera,
                viewportDataSource
            )
        }

        val routeOptions = RouteOptions.builder()
            .enableRefresh(transportMode == "moto")
            .bannerInstructions(true)
            .steps(true)
            .coordinatesList(listOf(origin, destination))
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .build()

        mapboxNavigation?.requestRoutes(routeOptions, routesRequestCallback = object : RouterCallback {
            override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                val event = Arguments.createMap()
                event.putString("onCancelNavigation", "Navigation Closed")

                sendEvent("onCancelNavigation", event)
            }

            override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {}

            override fun onRoutesReady(routes: List<DirectionsRoute>, routerOrigin: RouterOrigin) {
                viewportDataSource.onRouteChanged(routes.first())
                viewportDataSource.evaluate()

                mapboxNavigation?.setRoutes(routes, 0)
                mapboxNavigation?.startTripSession()

                registerObservers()

                sendEvent("onNavigationStarted", Arguments.createMap())

                if(shouldSimulateRoute) startSimulation(routes.first())

            }
        })
    }

    fun stopNavigation(camera: ReadableMap?) {
        unregisterObservers()
        mapboxMap?.getStyle()?.apply {
            // Render the result to update the map.
            routeLineApi.clearRouteLine { value ->
                routeLineView.renderClearRouteLineValue(
                    this,
                    value
                )
            }
        }

        // remove the route reference to change camera position
        viewportDataSource.clearRouteData()
        viewportDataSource.evaluate()

        if (shouldSimulateRoute) mapboxReplayer.finish()

        mapboxNavigation!!.setRoutes(listOf())
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.onDestroy()

        if (camera != null) {
            val mapAnimationOptions = MapAnimationOptions.Builder().duration(1500L).build()
            mapView.camera.easeTo(
                CameraOptions.Builder()
                    // Centers the camera to the lng/lat specified.
                    .center(Point.fromLngLat(
                        camera.getArray("center")!!.getDouble(1),
                        camera.getArray("center")!!.getDouble(0)))
                    .zoom(if(camera.hasKey("zoom")) camera.getDouble("zoom") else 15.0)
                    .pitch(0.0)
                    .padding(EdgeInsets(0.0, 0.0, 0.0, 0.0))
                    .build(),
                mapAnimationOptions
            )
        }
    }

    private fun registerObservers() {
        mapboxNavigation?.let {
            locationComponent.addOnIndicatorPositionChangedListener(onPositionChangedListener)
            mapboxNavigation!!.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation!!.registerLocationObserver(locationObserver)
            mapboxNavigation!!.registerRouteProgressObserver(replayProgressObserver)
            mapboxNavigation!!.getRerouteController()?.registerRerouteStateObserver(onRerouteObserver)
        }
    }

    private fun unregisterObservers() {
        mapboxNavigation?.let {
            locationComponent.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
            routeProgressObserver.let {
                mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver)
            }
            replayProgressObserver.let {
                mapboxNavigation?.unregisterRouteProgressObserver(replayProgressObserver)
            }
            locationObserver.let {
                mapboxNavigation?.unregisterLocationObserver(locationObserver)
            }
            onRerouteObserver.let {
                mapboxNavigation!!.getRerouteController()?.unregisterRerouteStateObserver(onRerouteObserver)
            }
        }
    }

    private fun updateCamera(point: Point, bearing: Double? = null, pitch: Double? = 45.0) {
        val mapAnimationOptions = MapAnimationOptions.Builder().duration(1500L).build()
        mapView.camera.easeTo(
            CameraOptions.Builder()
                // Centers the camera to the lng/lat specified.
                .center(point)
                // specifies the zoom value. Increase or decrease to zoom in or zoom out
                .zoom(17.0)
                // adjusts the bearing of the camera measured in degrees from true north
                .bearing(bearing)
                // adjusts the pitch towards the horizon
                .pitch(pitch)
                // specify frame of reference from the center.
                .padding(EdgeInsets(500.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptions
        )
    }

    private fun getTransportMode(transportMode: String): String {
        return when (transportMode) {
            "moto" -> DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            "pedestrian" -> DirectionsCriteria.PROFILE_WALKING
            "scooter" -> DirectionsCriteria.PROFILE_CYCLING
            else -> DirectionsCriteria.PROFILE_CYCLING
        }
    }

    private fun sendEvent(name: String, data: WritableMap) {
        context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, name, data)
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.run {
            stop()
            clearEvents()
            val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
            pushEvents(replayEvents)
            seekTo(replayEvents.first())
            play()
        }
    }

}
