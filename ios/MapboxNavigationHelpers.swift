import MapboxMaps
import MapboxDirections
import Foundation

func hexStringToUIColor (hex:String) -> UIColor {
    var cString:String = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

    if (cString.hasPrefix("#")) {
        cString.remove(at: cString.startIndex)
    }

    if ((cString.count) != 6) {
        return UIColor.gray
    }

    var rgbValue:UInt64 = 0
    Scanner(string: cString).scanHexInt64(&rgbValue)

    return UIColor(
        red: CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0,
        green: CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0,
        blue: CGFloat(rgbValue & 0x0000FF) / 255.0,
        alpha: CGFloat(1.0)
    )
}

func getImage(image: NSDictionary) -> UIImage {
    let uri = image.value(forKey: "uri") as! String
    let scale = image.value(forKey: "scale") as! CGFloat
    let imageUrl = URL(string: uri)
    let imageData = try? Data(contentsOf: imageUrl!)
    
    return UIImage(data: imageData!, scale: scale)!
}

func getTransportMode(transportMode: NSString) -> DirectionsProfileIdentifier {
    switch transportMode {
    case "moto":
        return .automobileAvoidingTraffic
    case "scooter":
        return .walking
    case "pedestrian":
        return .walking
    default:
        return .walking
    }
}

func hideMapInfo(_ mapView: MapView!) {
    mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    
    mapView.ornaments.options.scaleBar.visibility = .hidden
    mapView.ornaments.options.compass.visibility = .hidden
    mapView.ornaments.options.logo.margins = CGPoint(x: -80.0, y: -80.0)
    mapView.ornaments.options.attributionButton.margins = CGPoint(x: 100, y: 100)
    
    ResumeButton.appearance().alpha = 0
}
