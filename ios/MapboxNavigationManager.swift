import MapboxCoreNavigation
import MapboxMaps
import Foundation

@objc(MapboxNavigationManager)
class MapboxNavigationManager: RCTViewManager {
    var mapNavigationView: MapboxNavigationView? = nil
    
    override func view() -> UIView! {
        mapNavigationView = MapboxNavigationView()
        
        return mapNavigationView;
    }

    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
    
    @objc func startNavigation() {
        mapNavigationView?.startNavigation()
    }
    
    @objc func stopNavigation() {
        mapNavigationView?.stopNavigation()
    }
    
    @objc func startTracking() {
        mapNavigationView?.startTracking()
    }
    
    @objc func stopTracking() {
        mapNavigationView?.stopTracking()
    }
    
    @objc func setCamera(_ camera:NSDictionary) {
        if( mapNavigationView?.mapView != nil) {
            DispatchQueue.main.async {
                let center = (!(camera.value(forKey: "center") is NSNull) && camera.value(forKey: "center") != nil ? camera["center"] : self.mapNavigationView!.camera["center"]) as? Array<Double>
                
                self.mapNavigationView?.mapView?.camera.ease(
                    to: CameraOptions(
                        center:  CLLocationCoordinate2D(
                            latitude: center![0],
                            longitude: center![1]
                        ),
                        zoom: ((camera.value(forKey: "zoom") ?? self.mapNavigationView!.camera["zoom"]) as! CGFloat),
                        pitch: ((camera.value(forKey: "pitch") ?? self.mapNavigationView!.camera["pitch"]) as? CGFloat) ?? 0.0
                    ),
                duration: 0.5)
            }
        }
    }
}
