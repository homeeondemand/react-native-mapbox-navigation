package com.homee.mapboxnavigation

import android.util.Log
import android.widget.LinearLayout
import com.facebook.react.uimanager.ThemedReactContext
import com.mapbox.maps.MapView
import com.mapbox.maps.ResourceOptionsManager

class MapboxNavigationMapView(private val context: ThemedReactContext, private val viewGroup: LinearLayout): MapView(context.baseContext) {
    var mapView: MapView? = null

    fun initMap(): MapView? {
        var layout = inflate(context, R.layout.mapview_layout, viewGroup)

        mapView = layout.findViewById(R.id.mapView)

        return mapView
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

}