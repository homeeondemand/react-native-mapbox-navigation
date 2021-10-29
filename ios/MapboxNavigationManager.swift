@objc(MapboxNavigationManager)
class MapboxNavigationManager: RCTViewManager {
    var mapView: MapboxNavigationView? = nil
    
    override func view() -> UIView! {
        mapView = MapboxNavigationView()
        
        return mapView;
    }

    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
    
    @objc func stopNavigation() {
        mapView?.stopNavigation()
    }
}
