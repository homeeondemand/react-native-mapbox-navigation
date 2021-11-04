import MapboxCoreNavigation
import MapboxDirections
import MapboxNavigation
import MapboxMaps

extension UIView {
    var parentViewController: UIViewController? {
        var parentResponder: UIResponder? = self
        while parentResponder != nil {
            parentResponder = parentResponder!.next
            if let viewController = parentResponder as? UIViewController {
                return viewController
            }
        }
        return nil
    }
}

class MapboxNavigationView: UIView {
    weak var navViewController: NavigationViewController?
    internal var mapView: MapView!
    private var lineAnnotationManager: PolylineAnnotationManager?
    private var pointAnnotationManager: PointAnnotationManager?
    
    var embedded: Bool
    var embedding: Bool
    
    @objc var origin: NSArray = [] {
        didSet { setNeedsLayout() }
    }
    
    @objc var camera: NSDictionary = [:] {
        didSet { setNeedsLayout() }
    }
    
    @objc var destination: NSArray? = [] {
        didSet { setNeedsLayout() }
    }
    
    @objc var markers: NSArray = [] {
        didSet { setNeedsLayout() }
    }
    
    @objc var polyline: NSArray = [] {
        didSet { setNeedsLayout() }
    }
    
    @objc var shouldSimulateRoute: Bool = false
    @objc var showUserLocation: Bool = false
    @objc var styleURL: NSString = ""
    @objc var mapToken: NSString = ""
    @objc var transportMode: NSString = "bike"
    @objc var navigationToken: NSString = ""
    @objc var showsEndOfRouteFeedback: Bool = false
    @objc var destinationMarker: NSDictionary?
    @objc var userLocatorMap: NSDictionary?
    @objc var userLocatorNavigation: NSDictionary?
    @objc var onLocationChange: RCTDirectEventBlock?
    @objc var onRouteProgressChange: RCTDirectEventBlock?
    @objc var onError: RCTDirectEventBlock?
    @objc var onCancelNavigation: RCTDirectEventBlock?
    @objc var onArrive: RCTDirectEventBlock?
    @objc var onNavigationStarted: RCTDirectEventBlock?
    @objc var onTap: RCTDirectEventBlock?
    
    override init(frame: CGRect) {
        self.embedded = false
        self.embedding = false
        
        super.init(frame: frame)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        if self.destination?.count != 2 {
            return renderMap()
        }
        if (navViewController == nil && !embedding && !embedded) {
            embed()
        } else {
            navViewController?.view.frame = bounds
        }
    }
    
    override func removeFromSuperview() {
        super.removeFromSuperview()
        // cleanup and teardown any existing resources
        self.navViewController?.removeFromParent()
    }
    
    private func getImage(image: NSDictionary) -> UIImage {
        let uri = image.value(forKey: "uri") as! String
        let scale = image.value(forKey: "scale") as! CGFloat
        let imageUrl = URL(string: uri)
        let imageData = try? Data(contentsOf: imageUrl!)
        
        return UIImage(data: imageData!, scale: scale)!
    }
    
    private func getTransportMode(transportMode: NSString) -> DirectionsProfileIdentifier {
        switch transportMode {
        case "moto":
            return .automobile
        case "scooter":
            return .walking
        case "pedestrian":
            return .walking
        default:
            return .cycling
        }
    }
    
    private func renderMap() {
        guard origin.count == 2 else { return }
        
        let myMapInitOptions = MapInitOptions()
        
        ResourceOptionsManager.default.resourceOptions.accessToken = mapToken as String
        UserDefaults.standard.setValue(self.mapToken, forKey: "MBXAccessToken")
        
        mapView = MapView(frame: bounds, mapInitOptions: myMapInitOptions)
        
        mapView.ornaments.options.scaleBar.visibility = .hidden
        mapView.ornaments.options.compass.visibility = .hidden
        mapView.ornaments.options.logo.margins = CGPoint(x: -100, y: -100)
        mapView.ornaments.options.attributionButton.margins = CGPoint(x: -100, y: -100)
        
        if showUserLocation {
            if userLocatorMap != nil {
                var puck2DConfiguration = Puck2DConfiguration()
                
                puck2DConfiguration.topImage = getImage(image: userLocatorMap!)
                puck2DConfiguration.scale = .constant(1.0)
                 
                mapView.location.options.puckType = .puck2D(puck2DConfiguration)
            } else {
                mapView.location.options.puckType = .puck2D()
            }
        }
        if styleURL != "" , let styleUri = URL(string: styleURL as String) {
            mapView.mapboxMap.loadStyleURI(StyleURI.init(url: styleUri)!)
        }
        
        if camera["center"] != nil {
            let center = camera["center"] as! Array<Double>
            
            mapView.mapboxMap.setCamera(
                to: CameraOptions(
                    center: CLLocationCoordinate2D(
                        latitude: center[1],
                        longitude: center[0]
                    ),
                    zoom: camera["zoom"] as? CGFloat
                    
                )
            )
        }
        
        // Add the map.
        self.addSubview(mapView)
        
        mapView.mapboxMap.onNext(.mapLoaded) { [weak self] _ in
            self?.addPolyline()
            self?.addPoints()
        }
    }
    
    func addPolyline() {
        guard polyline.count > 0 else { return }
        
        var lineCoordinates: [CLLocationCoordinate2D] = []
        
        for p in polyline {
            let coords = p as! [CLLocationDegrees]
            
            lineCoordinates.append(CLLocationCoordinate2DMake(coords[0], coords[1]))
        }
        
        var polylineAnnotation = PolylineAnnotation(lineCoordinates: lineCoordinates)
        
        polylineAnnotation.lineColor = StyleColor(red: 0, green: 170, blue: 141, alpha: 1.0)
        polylineAnnotation.lineWidth = 4.0
        
        let lineAnnnotationManager = mapView.annotations.makePolylineAnnotationManager()
        
        lineAnnnotationManager.annotations = [polylineAnnotation]
        self.lineAnnotationManager = lineAnnnotationManager
        
        let camera = mapView.mapboxMap.camera(for: lineCoordinates,
                                              padding: .init(top: 42, left: 32, bottom: 148, right: 32),
                                              bearing: nil,
                                              pitch: nil)
        mapView.camera.ease(to: camera, duration: 0.5)
    }
    
    func addPoints() {
        guard markers.count > 0 else { return }
        
        var pointAnnotations: [PointAnnotation] = []
        
        for (index, m) in markers.enumerated() {
            if let marker = m as? Dictionary<String, Any> {
                
                var pointAnnotation = PointAnnotation(coordinate: CLLocationCoordinate2DMake(marker["latitude"]! as! CLLocationDegrees, marker["longitude"]! as! CLLocationDegrees))
                
                pointAnnotation.image = PointAnnotation.Image(image: getImage(image: marker["image"] as! NSDictionary), name: "marker" + String(index))
                
                pointAnnotations.append(pointAnnotation)
            }
        }
        
        self.pointAnnotationManager = self.mapView.annotations.makePointAnnotationManager()
        self.pointAnnotationManager?.annotations = pointAnnotations
        
    }
    
    private func embed() {
        guard origin.compactMap({ $0 }).count == 2 else { return }
        
        if destination?.compactMap({ $0 }).count == 2 {
            embedding = true
            
            let originWaypoint = Waypoint(coordinate: CLLocationCoordinate2D(latitude: CLLocationDegrees(origin[1] as! CGFloat), longitude: CLLocationDegrees(origin[0] as! CGFloat)))
            let destinationWaypoint = Waypoint(coordinate: CLLocationCoordinate2D(latitude: CLLocationDegrees(destination?[1] as! CGFloat), longitude: CLLocationDegrees(destination?[0] as! CGFloat)))
            
            let options = NavigationRouteOptions(waypoints: [originWaypoint, destinationWaypoint])
            options.profileIdentifier = getTransportMode(transportMode: transportMode)
            
            UserDefaults.standard.setValue(self.navigationToken, forKey: "MBXAccessToken")
            
            Directions.shared.calculate(options) { [weak self] (session, result) in
                guard let strongSelf = self, let parentVC = strongSelf.parentViewController else {
                    return
                }
                
                switch result {
                case .failure(let error):
                    print(error.localizedDescription)
                    strongSelf.onError!(["message": error.localizedDescription])
                case .success(let response):
                    guard response.routes!.count > 0 else {
                        return
                    }
                    
                    let navigationService = MapboxNavigationService(routeResponse: response, routeIndex: 0, routeOptions: options, simulating: strongSelf.shouldSimulateRoute ? .always : .onPoorGPS)
                    navigationService.simulationSpeedMultiplier = 5
                    
                    let navigationOptions = NavigationOptions()
                    
                    navigationOptions.navigationService = navigationService
                    navigationOptions.bottomBanner = MapboxNavigationBannerView()
                    navigationOptions.topBanner = MapboxNavigationBannerView()
                    
                    let vc = NavigationViewController(for: response, routeIndex: 0, routeOptions: options, navigationOptions: navigationOptions)
                    
                    if let mapView = vc.navigationMapView?.mapView {
                        let customViewportDataSource = MapboxNavigationViewportDataSource(mapView)
                        vc.navigationMapView?.navigationCamera.viewportDataSource = customViewportDataSource
                        
                        let customCameraStateTransition = MapboxNavigationCameraStateTransition(mapView)
                        vc.navigationMapView?.navigationCamera.cameraStateTransition = customCameraStateTransition
                        
                        if strongSelf.styleURL != "" , let styleUri = URL(string: strongSelf.styleURL as String) {
                            mapView.mapboxMap.loadStyleURI(StyleURI.init(url: styleUri)!)
                        }
                    }
                    
                    if strongSelf.userLocatorNavigation != nil {
                        var puck2DConfiguration = Puck2DConfiguration()
                        
                        puck2DConfiguration.topImage = strongSelf.getImage(image: (strongSelf.userLocatorNavigation)!)
                        puck2DConfiguration.scale = .constant(1.0)
                         
                        let userLocationStyle = UserLocationStyle.puck2D(configuration: puck2DConfiguration)
                        
                        vc.navigationMapView?.userLocationStyle = userLocationStyle
                    }
                    
                    vc.routeLineTracksTraversal = true
                    vc.navigationMapView?.routeCasingColor = #colorLiteral(red: 0.2078881264, green: 0.6503844261, blue: 0.5409962535, alpha: 1)
                    vc.navigationMapView?.traversedRouteColor = UIColor.clear
                    
                    vc.navigationMapView?.trafficLowColor = UIColor.clear
                    vc.navigationMapView?.trafficHeavyColor = UIColor.clear
                    vc.navigationMapView?.trafficSevereColor = UIColor.clear
                    vc.navigationMapView?.trafficUnknownColor = UIColor.clear
                    vc.navigationMapView?.trafficModerateColor = UIColor.clear
                    vc.navigationMapView?.alternativeTrafficLowColor = UIColor.clear
                    vc.navigationMapView?.alternativeTrafficHeavyColor = UIColor.clear
                    vc.navigationMapView?.alternativeTrafficSevereColor = UIColor.clear
                    vc.navigationMapView?.alternativeTrafficUnknownColor = UIColor.clear
                    vc.navigationMapView?.alternativeTrafficModerateColor = UIColor.clear
                    
                    vc.showsEndOfRouteFeedback = strongSelf.showsEndOfRouteFeedback
                    vc.showsReportFeedback = false
                    vc.showsSpeedLimits = false
                    vc.delegate = strongSelf
                    
                    
                    let tap = UITapGestureRecognizer(target: self, action: #selector(strongSelf.handleTap(_:)))
                    vc.view.isUserInteractionEnabled = true
                    vc.view.addGestureRecognizer(tap)
                    
                    parentVC.addChild(vc)
                    strongSelf.addSubview(vc.view)
                    vc.view.frame = strongSelf.bounds
                    vc.didMove(toParent: parentVC)
                    strongSelf.navViewController = vc
                    
                }
                
                strongSelf.embedding = false
                strongSelf.embedded = true
            }
        }
    }
    
    @objc func handleTap(_ sender: UITapGestureRecognizer? = nil) {
        onTap?(["message": ""]);
    }
    
    func stopNavigation() {

        DispatchQueue.main.async {
            self.navViewController?.navigationService.stop()
            self.navViewController?.dismiss(animated: false, completion: {})
        }
    }
    
}

// MARK: - NavigationMapViewDelegate methods

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
        pointAnnotationManager.annotations = [finalDestinationAnnotation]
    }
    
}

// MARK: - NavigationViewControllerDelegate methods
extension MapboxNavigationView: NavigationViewControllerDelegate {
    
    func navigationViewController(_ navigationViewController: NavigationViewController,
                                  didAdd finalDestinationAnnotation: PointAnnotation,
                                  pointAnnotationManager: PointAnnotationManager) {
        
        guard self.destinationMarker != nil else {
            return
        }
        
        var finalDestinationAnnotation = finalDestinationAnnotation
        finalDestinationAnnotation.image = PointAnnotation.Image(image: self.getImage(image: self.destinationMarker!), name: "destination_marker")
        
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
