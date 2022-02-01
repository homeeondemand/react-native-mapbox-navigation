/* eslint-disable comma-dangle */
import React from 'react';
import {StyleSheet, View} from 'react-native';
import MapboxNavigation from '@homee/react-native-mapbox-navigation';

const Navigation = props => {
  const {origin, destination} = props;

  return (
    <View style={styles.container}>
      <View style={styles.mapContainer}>
        <MapboxNavigation
          showsEndOfRouteFeedback={true}
          shouldSimulateRoute={true}
          origin={origin}
          destination={destination}
          showsEndOfRouteFeedback={false}
          hideStatusView
          onLocationChange={event => {
            console.log('onLocationChange', event.nativeEvent);
          }}
          onRouteProgressChange={event => {
            console.log('onRouteProgressChange', event.nativeEvent);
          }}
          onError={event => {
            const {message} = event.nativeEvent;
            // eslint-disable-next-line no-alert
            alert(message);
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
