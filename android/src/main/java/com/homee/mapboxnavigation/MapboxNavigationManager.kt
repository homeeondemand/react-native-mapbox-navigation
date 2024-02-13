package com.homee.mapboxnavigation

import android.content.pm.PackageManager
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.mapbox.geojson.Point
import com.mapbox.maps.ResourceOptionsManager
import com.mapbox.maps.TileStoreUsageMode
import javax.annotation.Nonnull

class MapboxNavigationManager(var mCallerContext: ReactApplicationContext) : SimpleViewManager<MapboxNavigationView>() {
  private var accessToken: String? = null
  init {
    mCallerContext.runOnUiQueueThread {
      try {
        val app = mCallerContext.packageManager.getApplicationInfo(mCallerContext.packageName, PackageManager.GET_META_DATA)
        val bundle = app.metaData
        val accessToken = bundle.getString("MAPBOX_ACCESS_TOKEN")
        this.accessToken = accessToken
        ResourceOptionsManager.getDefault(mCallerContext, accessToken).update {
          tileStoreUsageMode(TileStoreUsageMode.READ_ONLY)
        }
      } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
      }
    }
  }
  override fun getName(): String = "MapboxNavigation"

  public override fun createViewInstance(@Nonnull reactContext: ThemedReactContext): MapboxNavigationView {
    return MapboxNavigationView(reactContext, this.accessToken)
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
    )
  }

  @ReactProp(name = "origin")
  fun setOrigin(view: MapboxNavigationView, sources: ReadableArray?) {
    if (sources == null) {
      view.setOrigin(null)
      return
    }
    view.setOrigin(Point.fromLngLat(sources.getDouble(0), sources.getDouble(1)))
  }

  @ReactProp(name = "destination")
  fun setDestination(view: MapboxNavigationView, sources: ReadableArray?) {
    if (sources == null) {
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

  @ReactProp(name = "mute")
  fun setMute(view: MapboxNavigationView, mute: Boolean) {
    view.setMute(mute)
  }

  @ReactProp(name = "waypoints")
  fun setWaypoints(view: MapboxNavigationView, waypointsArray: ReadableArray?) {
      waypointsArray?.let {
          val waypoints = mutableListOf<Point>()
          for (i in 0 until it.size()) {
              val waypointArray = it.getArray(i)
              if (waypointArray !== null && waypointArray.size() >= 2) {
                  val longitude = waypointArray.getDouble(0)
                  val latitude = waypointArray.getDouble(1)
                  waypoints.add(Point.fromLngLat(longitude, latitude))
              }
          }

          view.setWaypoints(waypoints)
      }
  }

  @ReactProp(name = "vehicleMaxHeight")
  fun setMaxHeight(view: MapboxNavigationView, height: Int?) {
      if (height == null) {
        view.setMaxHeight(1.6)
        return
      }
      view.setMaxHeight(height?.toDouble())
  }
  
  @ReactProp(name = "vehicleMaxWidth")
  fun setMaxWidth(view: MapboxNavigationView, width: Int?) {
      if (width == null) {
        view.setMaxWidth(1.9)
        return
      }
      view.setMaxWidth(width?.toDouble())
  }


}