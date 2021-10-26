package com.homee.mapboxnavigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Transformations.map
import com.facebook.react.bridge.Arguments
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
import com.mapbox.maps.extension.style.expressions.dsl.generated.switchCase
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

class MapboxNavigationNavView(private val context: ThemedReactContext, private val token: String, private val id: Int, private val mapView: MapView) {
    var mapboxMap: MapboxMap? = null
    var mapboxNavigation: MapboxNavigation? = null
    var shouldSimulateRoute: Boolean = false

    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private lateinit var navigationCamera: NavigationCamera
    private val routeLineResources: RouteLineResources by lazy {
        var routeLineColorResources = RouteLineColorResources
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

        routeLineApi.setRoutes(
            routeLines
        ) { value ->
            mapboxMap?.getStyle()?.apply {
                routeLineView.renderRouteDrawData(this, value)
            }
        }
        routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            mapboxMap?.getStyle()?.apply {
                routeLineView.renderRouteLineUpdate(this, result)
            }
        }

        // RouteArrow: The next maneuver arrows are driven by route progress events.
        // Generate the next maneuver arrow update data and pass it to the view class
        // to visualize the updates on the map.
        //val arrowUpdate = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
        mapboxMap?.getStyle()?.apply {
            // Render the result to update the map.
            //routeArrowView.renderManeuverUpdate(this, arrowUpdate)
        }

        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        val rawManeuvers = maneuverApi.getManeuvers(routeProgress)

        var maneuvers = Arguments.createArray();
        var nextManeuver: WritableMap? = null;
        rawManeuvers?.value?.forEach { maneuver ->
            var formattedManeuver = Arguments.createMap()

            formattedManeuver.putDouble("distance", maneuver.stepDistance?.distanceRemaining ?: 0.0)
            formattedManeuver.putString("turn", maneuver.primary.modifier)
            formattedManeuver.putString("type", maneuver.primary.type)
            formattedManeuver.putString("exitNumber", maneuver.primary.id)
            formattedManeuver.putString("roadName", maneuver.primary.text)
            formattedManeuver.putString("instruction", maneuver.primary.text)

            if (nextManeuver == null) {
                nextManeuver = formattedManeuver
            } else {
                maneuvers.pushMap(formattedManeuver)
            }
        }

        val event = Arguments.createMap()

        event.putDouble("distanceTraveled", routeProgress.distanceTraveled.toDouble())
        event.putDouble("durationRemaining", routeProgress.durationRemaining)
        event.putDouble("fractionTraveled", routeProgress.fractionTraveled.toDouble())
        event.putDouble("distanceRemaining", routeProgress.distanceRemaining.toDouble())
        event.putDouble("eta", ((System.currentTimeMillis() / 1000) + routeProgress.durationRemaining))
        event.putDouble("expectedTravelTime", 0.0)
        event.putArray("maneuvers", maneuvers)
        event.putMap("nextManeuver", nextManeuver)

        sendEvent("onRouteProgressChange", event)
    }
    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        mapboxMap?.getStyle()?.apply {
            // Render the result to update the map.
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    private val navigationLocationProvider = NavigationLocationProvider()
    private val mapboxReplayer = MapboxReplayer()
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
    private val locationComponent by lazy {
        mapView?.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
    }

    fun initNavigation (userLocatorNavigation: Drawable?): MapboxNavigation? {
        mapView!!.location.apply {
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
            //.applyDefaultNavigationOptions()
            .bannerInstructions(true)
            .steps(true)
            .coordinatesList(listOf(origin, destination))
            .profile(getTransportMode(transportMode))
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


                mapboxNavigation.run {
                    mapboxNavigation?.startTripSession()
                    registerObservers()

                    // update the camera position to account for the new route
                    //viewportDataSource.onRouteChanged(routes.first())
                    //viewportDataSource.evaluate()
                }

                if(shouldSimulateRoute) startSimulation(routes.first())
                sendEvent("onNavigationStarted", Arguments.createMap())
            }
        })

    }

    fun stopNavigation() {
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.setRoutes(listOf())

        if (shouldSimulateRoute) mapboxReplayer.stop()

        unregisterObservers()

        MapboxNavigationProvider.destroy()
    }

    private fun registerObservers() {
        mapboxNavigation?.let {
            locationComponent.addOnIndicatorPositionChangedListener(onPositionChangedListener)
            mapboxNavigation!!.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation!!.registerLocationObserver(locationObserver)
            mapboxNavigation!!.registerRouteProgressObserver(replayProgressObserver)
        }
    }

    private fun unregisterObservers() {
        mapboxNavigation?.let {
            locationComponent.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
            routeProgressObserver?.let {
                mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver!!)
            }
            replayProgressObserver?.let {
                mapboxNavigation?.unregisterRouteProgressObserver(replayProgressObserver!!)
            }
            locationObserver?.let {
                mapboxNavigation?.unregisterLocationObserver(locationObserver!!)
            }
        }
    }

    private fun updateCamera(point: Point, bearing: Double? = null) {
        val mapAnimationOptions = MapAnimationOptions.Builder().duration(1500L).build()
        mapView?.camera.easeTo(
            CameraOptions.Builder()
                // Centers the camera to the lng/lat specified.
                .center(point)
                // specifies the zoom value. Increase or decrease to zoom in or zoom out
                .zoom(17.0)
                // adjusts the bearing of the camera measured in degrees from true north
                .bearing(bearing)
                // adjusts the pitch towards the horizon
                .pitch(45.0)
                // specify frame of reference from the center.
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptions
        )
    }

    private fun getTransportMode(transportMode: String): String {
        return when (transportMode) {
            "moto" -> DirectionsCriteria.PROFILE_DRIVING
            "scooter" -> DirectionsCriteria.PROFILE_WALKING
            "pedestrian" -> DirectionsCriteria.PROFILE_WALKING
            else -> DirectionsCriteria.PROFILE_CYCLING
        }
    }

    private fun sendEvent(name: String, data: WritableMap) {
        context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, name, data)
    }

//    private fun getInitialCameraPosition(): CameraPosition {
//        return CameraPosition.Builder()
//            .zoom(15.0)
//            .build()
//    }

//     override fun onNavigationFinished() {

//     }

//     override fun onNavigationRunning() {

//     }

//     override fun onNavigationReady(isRunning: Boolean) {
//         //try {
// //            print("-------$navigationToken")
// //            val accessToken = navigationToken
// //            if (accessToken == null) {
// //                sendErrorToReact("Mapbox access token is not set")
// //                return
// //            }
// //
// //            if (origin == null || destination == null) {
// //                sendErrorToReact("origin and destination are required")
// //                return
// //            }
// //
// //            if (::navigationMapboxMap.isInitialized) {
// //                return
// //            }
// //
// //            //if (this.retrieveNavigationMapboxMap() == null) {
// //            //    sendErrorToReact("retrieveNavigationMapboxMap() is null")
// //            //    return
// //            //}
// //
// //            //this.navigationMapboxMap = this.retrieveNavigationMapboxMap()!!
// //
// //            //this.retrieveMapboxNavigation()?.let { this.mapboxNavigation = it } // this does not work
// //
// //            // fetch the route
// //            val navigationOptions = MapboxNavigation
// //                .defaultNavigationOptionsBuilder(context, accessToken)
// //                .isFromNavigationUi(true)
// //                .build()
// //            //this.mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
// //            //this.mapboxNavigation.requestRoutes(RouteOptions.builder()
// //            //        .applyDefaultParams()
// //            //        .accessToken(accessToken)
// //            //        .coordinates(mutableListOf(origin, destination))
// //            //        .profile(RouteUrl.PROFILE_DRIVING)
// //            //        .steps(true)
// //            //        .voiceInstructions(true)
// //            //        .build(), routesReqCallback)
//         //} catch (ex: Exception) {
// //            sendErrorToReact(ex.toString())
//         //}
//     }

    fun onFinalDestinationArrival(enableDetailedFeedbackFlowAfterTbt: Boolean, enableArrivalExperienceFeedback: Boolean) {
        //super.onFinalDestinationArrival(this.showsEndOfRouteFeedback, this.showsEndOfRouteFeedback)
        val event = Arguments.createMap()
        event.putString("onArrive", "")
        context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onArrive", event)
    }

    private fun startSimulation(route: DirectionsRoute) {
        Log.w("-----", "startSimulation")
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
