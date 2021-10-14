import MapboxNavigation
import MapboxMaps

class MapboxNavigationCameraStateTransition: CameraStateTransition {
    
    weak var mapView: MapView?
    
    required init(_ mapView: MapView) {
        self.mapView = mapView
    }
    
    func transitionToFollowing(_ cameraOptions: CameraOptions, completion: @escaping (() -> Void)) {
        mapView?.camera.ease(to: cameraOptions, duration: 0.5, curve: .linear, completion: { _ in
            completion()
        })
    }
    
    func transitionToOverview(_ cameraOptions: CameraOptions, completion: @escaping (() -> Void)) {
        mapView?.camera.ease(to: cameraOptions, duration: 0.5, curve: .linear, completion: { _ in
            completion()
        })
    }
    
    func update(to cameraOptions: CameraOptions, state: NavigationCameraState) {
        mapView?.camera.ease(to: cameraOptions, duration: 0.5, curve: .linear, completion: nil)
    }
    
    func cancelPendingTransition() {
        mapView?.camera.cancelAnimations()
    }
}
