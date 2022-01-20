package com.homee.mapboxnavigation

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
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
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
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
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
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
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.*
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import java.util.*

class MapboxNavigationNavigation(private val context:ThemedReactContext, private val token: String, private val id: Int, private val mapView: MapView) {
    public var followUser: Boolean = false
    private var shouldSimulate: Boolean = false
    private var useImperial: Boolean = false
    private var transportMode: String = "moto"
    private var mapboxMap: MapboxMap? = null
    private var mapboxNavigation: MapboxNavigation? = null

    /**
     * [NavigationLocationProvider] is a utility class that helps to provide location updates generated by the Navigation SDK
     * to the Maps SDK in order to update the user location indicator on the map.
     */
    private val navigationLocationProvider = NavigationLocationProvider()

    /**
     * Debug tool used to play, pause and seek route progress events that can be used to produce mocked location updates along the route.
     */
    private val mapboxReplayer = MapboxReplayer()

    /**
     * Debug tool that mocks location updates with an input from the [mapboxReplayer].
     */
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)

    /**
     * Debug observer that makes sure the replayer has always an up-to-date information to generate mock updates.
     */
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    /**
     * Used to execute camera transitions based on the data generated by the [viewportDataSource].
     * This includes transitions from route overview to route following and continuously updating the camera as the location changes.
     */
    private lateinit var navigationCamera: NavigationCamera

    /**
     * Produces the camera frames based on the location and routing data for the [navigationCamera] to execute.
     */
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    /**
     * Generates updates for the MapboxManeuverView to display the upcoming maneuver instructions
     * and remaining distance to the maneuver point.
     */
    private lateinit var maneuverApi: MapboxManeuverApi

    /**
     * Generates updates for the MapboxTripProgressView that include remaining time and distance to the destination.
     */
    private lateinit var tripProgressApi: MapboxTripProgressApi

    /**
     * Extracts message that should be communicated to the driver about the upcoming maneuver.
     * When possible, downloads a synthesized audio file that can be played back to the driver.
     */
    private lateinit var speechApi: MapboxSpeechApi

    /**
     * Plays the synthesized audio files with upcoming maneuver instructions
     * or uses an on-device Text-To-Speech engine to communicate the message to the driver.
     */
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer

    /**
     * Observes when a new voice instruction should be played.
     */
    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        speechApi.generate(voiceInstructions, speechCallback)
    }

    private val voiceInstructionsPlayerCallback = MapboxNavigationConsumer<SpeechAnnouncement> { value ->
        speechApi.clean(value)
    }

    /**
     * Based on whether the synthesized audio file is available, the callback plays the file
     * or uses the fall back which is played back using the on-device Text-To-Speech engine.
     */
    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
                    // play the instruction via fallback text-to-speech engine
                    voiceInstructionsPlayer.play(
                            error.fallback,
                            voiceInstructionsPlayerCallback
                    )
                },
                { value ->
                    // The announcement data obtained (synthesized speech mp3 file from Mapbox's API Voice) is played
                    // using [MapboxVoiceInstructionsPlayer]
                    voiceInstructionsPlayer.play(
                            value.announcement,
                            voiceInstructionsPlayerCallback
                    )
                }
            )
        }

    private val locationComponent by lazy {
        mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
    }

    // initialize route line, the withRouteLineBelowLayerId is specified to place
    // the route line below road labels layer on the map
    // the value of this option will depend on the style that you are using
    // and under which layer the route line should be placed on the map layers stack
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
    private val mapboxRouteLineOptions: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(context.baseContext)
            .withVanishingRouteLineEnabled(true)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label")
            .build()
    }

    /**
     * Generates updates for the [routeLineView] with the geometries and properties of the routes that should be drawn on the map.
     */
    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(mapboxRouteLineOptions)
    }

    /**
     * Draws route lines on the map based on the data from the [routeLineApi]
     */
    private val routeLineView: MapboxRouteLineView by lazy {
        MapboxRouteLineView(mapboxRouteLineOptions)
    }

    /**
     * Generates updates for the [routeArrowView] with the geometries and properties of maneuver arrows that should be drawn on the map.
     */
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()

    /**
     * Draws maneuver arrows on the map based on the data [routeArrowApi].
     */
    private lateinit var routeArrowView: MapboxRouteArrowView

    /**
     * Gets notified with location updates.
     *
     * Exposes raw updates coming directly from the location services
     * and the updates enhanced by the Navigation SDK (cleaned up and matched to the road).
     */
    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {}
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation

            // update location puck's position on the map
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            // update camera position to account for new location
//            viewportDataSource.onLocationChanged(enhancedLocation)
//            viewportDataSource.evaluate()

            if (followUser) {
                updateCamera(
                    Point.fromLngLat(
                        enhancedLocation.longitude,
                        enhancedLocation.latitude
                    ),
                    enhancedLocation.bearing.toDouble()
                )
            }
        }
    }

    /**
     * Gets notified with progress along the currently active route.
     */
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
        // update the camera position to account for the progressed fragment of the route
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        // draw the upcoming maneuver arrow on the map
        val style = mapboxMap?.getStyle()
        if (style != null) {
            val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
        }

        // update top banner with maneuver instructions
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

        val event = Arguments.createMap()
        event.putDouble("latitude", point.latitude())
        event.putDouble("longitude", point.longitude())
        sendEvent("onLocationChange", event)
    }

    /**
     * Gets notified whenever the user get off the track and route is regenerated
     */
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

    @SuppressLint("MissingPermission")
    fun startNavigation(origin: Point, destination: Point, transportMode: String, shouldSimulate: Boolean, useImperial: Boolean, language: String, voiceEnabled: Boolean, userLocatorNavigation: Drawable?) {
        updateCamera(origin)
        this.shouldSimulate = shouldSimulate
        this.transportMode = transportMode
        this.useImperial = useImperial

        mapboxMap = mapView.getMapboxMap()

        mapView.location.apply {
            setLocationProvider(navigationLocationProvider)

            if (userLocatorNavigation != null) {
                locationPuck = LocationPuck2D(
                        bearingImage = userLocatorNavigation,
                )
            }
            enabled = true
        }

        // initialize the location puck
//        mapView.location.apply {
//            setLocationProvider(navigationLocationProvider)
//            enabled = true
//        }

        // initialize Mapbox Navigation
        mapboxNavigation = if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve()
        } else {
            val formatterOptions = DistanceFormatterOptions
                .Builder(context.baseContext)
                .unitType(
                    if (useImperial) UnitType.IMPERIAL else UnitType.METRIC
                )
                .build()
            val providerOptions = NavigationOptions.Builder(context.baseContext)
               .distanceFormatterOptions(formatterOptions)
                .accessToken(token)

            if (shouldSimulate) {
                providerOptions.locationEngine(replayLocationEngine)
            }

            MapboxNavigationProvider.create(
               providerOptions
                   .build()
            )
        }

        // initialize Navigation Camera
        viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap!!)
        navigationCamera = NavigationCamera(
            mapboxMap!!,
            mapView.camera,
            viewportDataSource
        )

        // set the animations lifecycle listener to ensure the NavigationCamera stops
        // automatically following the user location when the map is interacted with
        mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )

        // make sure to use the same DistanceFormatterOptions across different features
        val distanceFormatterOptions = mapboxNavigation!!.navigationOptions.distanceFormatterOptions

        // initialize maneuver api that feeds the data to the top banner maneuver view
        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(distanceFormatterOptions)
        )

        // initialize bottom progress view
        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(context)
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(distanceFormatterOptions)
                )
                .timeRemainingFormatter(
                    TimeRemainingFormatter(context)
                )
                .percentRouteTraveledFormatter(
                    PercentDistanceTraveledFormatter()
                )
                .estimatedTimeToArrivalFormatter(
                    EstimatedTimeToArrivalFormatter(context, TimeFormat.NONE_SPECIFIED)
                )
                .build()
        )

        // initialize voice instructions api and the voice instruction player
        speechApi = MapboxSpeechApi(
                context.applicationContext,
                token,
                language
        )
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
                context.applicationContext,
                token,
                language
        )

        // initialize maneuver arrow view to draw arrows on the map
        val routeArrowOptions = RouteArrowOptions.Builder(context).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)

        // start the trip session to being receiving location updates in free drive
        // and later when a route is set also receiving route progress updates
        mapboxNavigation!!.startTripSession()

        generateRoute(origin, destination, language, voiceEnabled)
    }

    fun stopNavigation() {
        // clear
        mapboxNavigation?.setRoutes(listOf())
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.onDestroy()

        // stop simulation
        if (shouldSimulate) mapboxReplayer.finish()

        mapboxMap?.getStyle()?.apply {
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

//        val lastLocation = navigationLocationProvider.lastLocation
//        lastLocation?.let {
//            updateCamera(lastLocation.toPoint(), 0.0, 0.0)
//        }

        unregisterListeners()
    }

    private fun generateRoute(origin: Point, destination: Point, language: String, voiceEnabled: Boolean) {
        val routeOptions = RouteOptions.builder()
            .profile(getTransportMode(transportMode))
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .voiceUnits(if(useImperial) DirectionsCriteria.IMPERIAL else DirectionsCriteria.METRIC)
            .steps(true)
            .language(language)
            .continueStraight(true)
            .voiceInstructions(voiceEnabled)
            .bannerInstructions(true)
            .coordinatesList(listOf(origin, destination))
            .build()

        // execute a route request
        // it's recommended to use the
        // applyDefaultNavigationOptions and applyLanguageAndVoiceUnitOptions
        // that make sure the route request is optimized
        // to allow for support of all of the Navigation SDK features
        mapboxNavigation?.requestRoutes(routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    setRouteAndStartNavigation(routes)
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {}

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {}
            }
        )
    }

    private fun setRouteAndStartNavigation(routes: List<DirectionsRoute>) {
        // set routes, where the first route in the list is the primary route that
        // will be used for active guidance
        mapboxNavigation?.setRoutes(routes, 0)

        // start location simulation along the primary route
        if (shouldSimulate) startSimulation(routes.first())

        viewportDataSource.onRouteChanged(routes.first())
        viewportDataSource.evaluate()

        sendEvent("onNavigationStarted", Arguments.createMap())

        registerListeners()
    }

    private fun getTransportMode(transportMode: String): String {
        return when (transportMode) {
            "moto" -> DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            "pedestrian" -> DirectionsCriteria.PROFILE_WALKING
            "scooter" -> DirectionsCriteria.PROFILE_CYCLING
            else -> DirectionsCriteria.PROFILE_CYCLING
        }
    }

    private fun updateCamera(point: Point, bearing: Double? = null, pitch: Double? = 45.0) {
        val mapAnimationOptions = MapAnimationOptions.Builder().duration(500L).build()
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

    private fun registerListeners() {
        // register event listeners
        locationComponent.addOnIndicatorPositionChangedListener(onPositionChangedListener)
        mapboxNavigation?.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.registerLocationObserver(locationObserver)
        mapboxNavigation?.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation?.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation?.getRerouteController()?.registerRerouteStateObserver(onRerouteObserver)
    }

    private fun unregisterListeners() {
        // unregister event listeners to prevent leaks or unnecessary resource consumption
        locationComponent.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.unregisterLocationObserver(locationObserver)
        mapboxNavigation?.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation?.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation?.getRerouteController()?.unregisterRerouteStateObserver(onRerouteObserver)
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
