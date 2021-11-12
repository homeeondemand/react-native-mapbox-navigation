import MapboxCoreNavigation
import MapboxDirections
import MapboxNavigation
import MapboxMaps
import Foundation

extension MapboxNavigationView: NavigationMapViewDelegate {
    
    // Delegate method, which is called whenever final destination `PointAnnotation` is added on
    // `MapView`.
    func navigationMapView(_ navigationMapView: NavigationMapView,
                           didAdd finalDestinationAnnotation: PointAnnotation,
                           pointAnnotationManager: PointAnnotationManager) {
        var finalDestinationAnnotation = finalDestinationAnnotation
        if let image = UIImage(named: "marker") {
            finalDestinationAnnotation.image = PointAnnotation.Image(image: image, name: "marker")
        }
        
        // `PointAnnotationManager` is used to manage `PointAnnotation`s and is also exposed as
        // a property in `NavigationMapView.pointAnnotationManager`. After any modifications to the
        // `PointAnnotation` changes must be applied to `PointAnnotationManager.annotations`
        // array. To remove all annotations for specific `PointAnnotationManager`, set an empty array.
        //pointAnnotationManager.annotations = [finalDestinationAnnotation]
    }
    
}

extension MapboxNavigationView: NavigationViewControllerDelegate {
    
    func navigationViewController(_ navigationViewController: NavigationViewController,
                                  didAdd finalDestinationAnnotation: PointAnnotation,
                                  pointAnnotationManager: PointAnnotationManager) {
        
        guard self.destinationMarker != nil else {
            return
        }
        
        var finalDestinationAnnotation = finalDestinationAnnotation
        finalDestinationAnnotation.image = PointAnnotation.Image(image: getImage(image: self.destinationMarker!), name: "destination_marker")
        
        pointAnnotationManager.annotations = [finalDestinationAnnotation]
    }
    
    func navigationViewControllerDidDismiss(_ navigationViewController: NavigationViewController, byCanceling canceled: Bool) {
        if (!canceled) {
            return;
        }
        
        onCancelNavigation?(["message": ""]);
    }
    
    func navigationViewController(_ navigationViewController: NavigationViewController, didArriveAt waypoint: Waypoint) -> Bool {
        onArrive?(["message": ""]);
        return true;
    }
    
    func navigationViewController(_ navigationViewController: NavigationViewController, willRerouteFrom location: CLLocation?) {
        onReroute?(["message": "reroute"]);
    }
    
    func navigationViewController(_ navigationViewController: NavigationViewController, didRerouteAlong route: Route) {
        onReroute?(["message": "done"]);
    }
    
    func navigationViewController(_ navigationViewController: NavigationViewController, didUpdate progress: RouteProgress, with location: CLLocation, rawLocation: CLLocation) {
        var maneuvers : [[AnyHashable : Any]] = []
        if(navigationViewController.route != nil) {
            for leg in navigationViewController.route!.legs {
                for step in leg.steps {
                    maneuvers.append(formatManeuver(maneuver: step))
                }
            }
        }
        
        onLocationChange?(["longitude": location.coordinate.longitude, "latitude": location.coordinate.latitude])
        
        onRouteProgressChange?(["distanceTraveled": progress.distanceTraveled,
                                "stepDistanceRemaining": progress.currentLegProgress.currentStepProgress.distanceRemaining,
                                "durationRemaining": progress.durationRemaining,
                                "eta": NSDate().timeIntervalSince1970 + progress.durationRemaining,
                                "expectedTravelTime": progress.route.expectedTravelTime,
                                "fractionTraveled": progress.fractionTraveled,
                                "distanceRemaining": progress.distanceRemaining,
                                "maneuvers": maneuvers,
                                "route": navigationViewController.routeResponse.identifier,
                                "stepIndex": navigationViewController.navigationService.routeProgress.currentLegProgress.stepIndex,
        ])
        onNavigationStarted?([:])
    }
    
    private func formatManeuver(maneuver: RouteStep) -> [AnyHashable : Any] {
        return [
            "distance": maneuver.distance,
            "turn": (maneuver.maneuverDirection?.rawValue ?? "") as String,
            "type": maneuver.maneuverType.rawValue,
            "exitNumber": maneuver.exitIndex,
            "roadName": (maneuver.names?.first?.description ?? "") as String,
            "instruction": maneuver.instructions,
        ]
    }
}
