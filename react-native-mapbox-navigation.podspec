require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-mapbox-navigation"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = <<-DESC
                  Smart Mapbox turn-by-turn routing based on real-time traffic for React Native.
                   DESC
  s.homepage     = "https://github.com/homeeondemand/react-native-mapbox-navigation"
  s.license    = { :type => "MIT", :file => "LICENSE" }
  s.authors      = { "HOMEE" => "support@homee.com" }
  s.platforms    = { :ios => "10.0" }
  s.source       = { :git => "https://github.com/homeeondemand/react-native-mapbox-navigation.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,swift}"
  s.requires_arc = true

  s.dependency "React-Core"
  # For more info on why "@react-native-mapbox-gl-mapbox-static" is required,
  # check out this PR on the react-native-mapbox-gl/maps repo
  # https://github.com/react-native-mapbox-gl/maps/pull/714
  s.dependency '@react-native-mapbox-gl-mapbox-static', '6.3.0'
  s.dependency "MapboxNavigation", "~> 1.2.1"
end

