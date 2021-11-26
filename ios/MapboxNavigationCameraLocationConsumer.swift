import MapboxCoreNavigation
import MapboxMaps

public class MapboxNavigationCameraLocationConsumer: LocationConsumer {
    weak var mapView: MapView?
 
    init(mapView: MapView) {
        self.mapView = mapView
    }
     
    public func locationUpdate(newLocation: Location) {
        mapView?.camera.ease(
            to: CameraOptions(center: newLocation.coordinate, zoom: 15, bearing: newLocation.headingDirection),
            duration: 1.3
        )
    }
}
