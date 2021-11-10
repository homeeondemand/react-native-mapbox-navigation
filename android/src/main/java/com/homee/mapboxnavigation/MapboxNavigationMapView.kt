package com.homee.mapboxnavigation

import android.view.MotionEvent
import android.widget.LinearLayout
import com.facebook.react.bridge.Arguments
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.mapbox.maps.MapView

class MapboxNavigationMapView(private val context: ThemedReactContext, private val viewGroup: LinearLayout, private val viewId: Int): MapView(context.baseContext) {
    var mapView: MapView? = null

    fun initMap(): MapView? {
        var layout = inflate(context, R.layout.mapview_layout, viewGroup)

        mapView = layout.findViewById(R.id.mapView)

        mapView?.setOnTouchListener(OnTouchListener { view, motionEvent ->
            if(motionEvent.action == MotionEvent.ACTION_UP) {
                val event = Arguments.createMap()
                event.putString("onTap", "")
                context.getJSModule(RCTEventEmitter::class.java).receiveEvent(viewId, "onTap", event)
            }
            false
        })

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