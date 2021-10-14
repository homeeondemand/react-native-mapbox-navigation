import MapboxCoreNavigation
import MapboxNavigation
import MapboxMaps

class MapboxNavigationViewportDataSource: ViewportDataSource {
    
    public weak var delegate: ViewportDataSourceDelegate?
    
    public var followingMobileCamera: CameraOptions = CameraOptions()
    
    public var followingCarPlayCamera: CameraOptions = CameraOptions()
    
    public var overviewMobileCamera: CameraOptions = CameraOptions()
    
    public var overviewCarPlayCamera: CameraOptions = CameraOptions()
    
    weak var mapView: MapView?
    
    // MARK: - Initializer methods
    
    public required init(_ mapView: MapView) {
        self.mapView = mapView
        self.mapView?.location.addLocationConsumer(newConsumer: self)
        
        subscribeForNotifications()
    }
    
    deinit {
        unsubscribeFromNotifications()
    }
    
    // MARK: - Notifications observer methods
    
    func subscribeForNotifications() {
        // `MapboxNavigationViewportDataSource` uses raw locations provided by `LocationConsumer` in
        // free-drive mode and locations snapped to the road provided by
        // `Notification.Name.routeControllerProgressDidChange` notification.
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(progressDidChange(_:)),
                                               name: .routeControllerProgressDidChange,
                                               object: nil)
    }
    
    func unsubscribeFromNotifications() {
        NotificationCenter.default.removeObserver(self,
                                                  name: .routeControllerProgressDidChange,
                                                  object: nil)
    }
    
    @objc func progressDidChange(_ notification: NSNotification) {
        let location = notification.userInfo?[RouteController.NotificationUserInfoKey.locationKey] as? CLLocation
        let routeProgress = notification.userInfo?[RouteController.NotificationUserInfoKey.routeProgressKey] as? RouteProgress
        let cameraOptions = self.cameraOptions(location, routeProgress: routeProgress)
        
        delegate?.viewportDataSource(self, didUpdate: cameraOptions)
    }
    
    func cameraOptions(_ location: CLLocation?, routeProgress: RouteProgress? = nil) -> [String: CameraOptions] {
        followingMobileCamera.center = location?.coordinate
        // Set the bearing of the `MapView` (measured in degrees clockwise from true north).
        followingMobileCamera.bearing = location?.course
        followingMobileCamera.padding = .zero
        followingMobileCamera.zoom = 15.0
        followingMobileCamera.pitch = 45.0
        
        if let shape = routeProgress?.route.shape,
           let camera = mapView?.mapboxMap.camera(for: .lineString(shape),
                                                  padding: UIEdgeInsets(top: 150.0, left: 10.0, bottom: 150.0, right: 10.0),
                                                  bearing: 0.0,
                                                  pitch: 0.0) {
            overviewMobileCamera = camera
        }
        
        let cameraOptions = [
            CameraOptions.followingMobileCamera: followingMobileCamera,
            CameraOptions.overviewMobileCamera: overviewMobileCamera
        ]
        
        return cameraOptions
    }
}

// MARK: - LocationConsumer delegate

extension MapboxNavigationViewportDataSource: LocationConsumer {
    
    var shouldTrackLocation: Bool {
        return true
    }
    
    func locationUpdate(newLocation: Location) {
        let location = CLLocation(coordinate: newLocation.coordinate,
                                  altitude: 0.0,
                                  horizontalAccuracy: newLocation.horizontalAccuracy,
                                  verticalAccuracy: 0.0,
                                  course: newLocation.course,
                                  speed: 0.0,
                                  timestamp: Date())
        
        let cameraOptions = self.cameraOptions(location)
        delegate?.viewportDataSource(self, didUpdate: cameraOptions)
    }
}
