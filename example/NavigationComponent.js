/* eslint-disable comma-dangle */
import React from 'react';
import {StyleSheet, View} from 'react-native';
import MapboxNavigation from '@homee/react-native-mapbox-navigation';

const Navigation = props => {
  const {origin, destination, customRoutes} = props;

  return (
    <View style={styles.container}>
      <View style={styles.mapContainer}>
        <MapboxNavigation
          shouldSimulateRoute={false}
          origin={origin}
          destination={destination}
          customRoutes={customRoutes}
          showsEndOfRouteFeedback={false}
          hideStatusView
          onLocationChange={event => {
            console.log('onLocationChange', event.nativeEvent);
          }}
          onRouteProgressChange={event => {
            console.log('onRouteProgressChange', event.nativeEvent);
          }}
          onError={event => {
            const {error} = event.nativeEvent;
            // eslint-disable-next-line no-alert
            console.log({error});
            alert('lol' + error);
          }}
          onArrive={() => {
            // eslint-disable-next-line no-alert
            alert('You have reached your destination');
          }}
          onCancelNavigation={event => {
            alert('Cancelled navigation event');
          }}
        />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
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
});

export default Navigation;
