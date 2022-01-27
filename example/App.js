/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React, {useState, useLayoutEffect} from 'react';
import {SafeAreaView, useColorScheme} from 'react-native';
import {Colors} from 'react-native/Libraries/NewAppScreen';
import NavigationComponent from './NavigationComponent';
import {PermissionsAndroid} from 'react-native';

const App = () => {
  const [locationPermissionGranted, setLocationPermissionGranted] =
    useState(false);

  const isDarkMode = useColorScheme() === 'dark';

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
    flex: 1,
  };

  useLayoutEffect(async () => {
    async function requestLocationPermission() {
      try {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          {
            title: 'Example App',
            message: 'Example App access to your location ',
          },
        );
        if (granted) {
          setLocationPermissionGranted(true);
        }
      } catch (err) {
        console.warn(err);
      }
    }

    requestLocationPermission();
  }, []);

  return (
    <SafeAreaView style={backgroundStyle}>
      <NavigationComponent
        origin={[-100.2487974, 25.5977387]}
        destination={[-100.285876, 25.639647]}
        onCancelNavigation={() => alert('Cancel button pressed')}
      />
    </SafeAreaView>
  );
};

export default App;
