/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React, {useEffect} from 'react';
import {SafeAreaView, useColorScheme} from 'react-native';
import {Colors} from 'react-native/Libraries/NewAppScreen';
import NavigationComponent from './NavigationComponent';
import {PermissionsAndroid} from 'react-native';

const App = () => {
  const isDarkMode = useColorScheme() === 'dark';

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
    flex: 1,
  };

  useEffect(() => {
    const requestLocationPermission = async () => {
      try {
        await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          {
            title: 'Example App',
            message: 'Example App access to your location ',
          },
        );
      } catch (err) {
        console.warn(err);
      }
    };

    requestLocationPermission();
  }, []);

  return (
    <SafeAreaView style={backgroundStyle}>
      <NavigationComponent
        origin={[67.0785205, 24.8872153]}
        destination={[67.07814, 24.885666]}
        wayPointNames={'Starting Point;;;;;;;;;;;;;;;;;;;;;;;;;;;;;Destination'}
        customRoutes={[
          [67.0785205, 24.8872153],
          [67.063154, 24.849818],
          [67.063386, 24.849607],
          [67.063233, 24.849184],
          [67.060686, 24.847349],
          [67.05853, 24.847615],
          [67.056401, 24.847578],
          [67.054741, 24.847661],
          [67.053531, 24.850061],
          [67.052048, 24.853742],
          [67.051127, 24.856007],
          [67.050128, 24.858714],
          [67.053542, 24.859905],
          [67.059572, 24.859769],
          [67.065762, 24.86125],
          [67.067911, 24.863602],
          [67.066797, 24.867586],
          [67.066727, 24.869255],
          [67.068641, 24.87041],
          [67.068176, 24.872092],
          [67.069879, 24.874289],
          [67.071523, 24.87652],
          [67.073738, 24.879734],
          [67.074686, 24.884207],
          [67.075582, 24.88621],
          [67.077448, 24.887272],
          [67.078639, 24.886928],
          [67.078646, 24.886934],
          [67.078444, 24.887264],
          [67.07814, 24.885666],
        ]}
      />
    </SafeAreaView>
  );
};

export default App;
