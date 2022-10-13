import React, {useState} from 'react';
import {
  TouchableOpacity,
  SafeAreaView,
  StyleSheet,
  View,
  Text,
} from 'react-native';
import MapView from 'react-native-maps';
import NavigationComponent from './NavigationComponent';

const App = () => {
  const [view, setView] = useState('map');
  const toggle = () => setView(v => (v === 'map' ? 'navigation' : 'map'));
  return (
    <SafeAreaView style={styles.navigation}>
      <TouchableOpacity onPress={toggle} style={styles.button}>
        <Text>Toggle</Text>
      </TouchableOpacity>
      {view === 'map' ? (
        <MapView
          style={styles.map}
          initialRegion={{
            latitude: 37.78825,
            longitude: -122.4324,
            latitudeDelta: 0.0922,
            longitudeDelta: 0.0421,
          }}
        />
      ) : (
        <View style={styles.container}>
          <View style={styles.mapContainer}>
            <NavigationComponent
              origin={[-105.140629, 39.760194]}
              destination={[-105.156544, 39.761801]}
            />
          </View>
        </View>
      )}
    </SafeAreaView>
  );
};

export default App;

const styles = StyleSheet.create({
  navigation: {
    flex: 1,
    backgroundColor: 'red',
  },
  button: {
    position: 'absolute',
    backgroundColor: 'white',
    top: 80,
    right: 0,
    left: 0,
    zIndex: 10,
    padding: 20,
  },
  map: {
    ...StyleSheet.absoluteFillObject,
  },
  container: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between',
    height: '100%',
  },
  mapContainer: {
    flex: 1,
  },
  page: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
});
