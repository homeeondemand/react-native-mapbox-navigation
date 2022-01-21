/* eslint-disable comma-dangle */
import React from 'react';
import {StyleSheet, View} from 'react-native';
import MapboxNavigation from '@homee/react-native-mapbox-navigation';

const Navigation = props => {
  const {origin, destination, onCancelNavigation = undefined} = props;

  return (
    <View style={styles.container}>
      <View style={styles.mapContainer}>
        <MapboxNavigation
          origin={origin}
          destination={destination}
          showsEndOfRouteFeedback={false}
          hideStatusView
          onLocationChange={_event => {
            // const { latitude, longitude } = event.nativeEvent;
          }}
          onRouteProgressChange={_event => {
            // const {
            //   distanceTraveled,
            //   durationRemaining,d
            //   fractionTraveled,
            //   distanceRemaining
            // } = event.nativeEvent;
          }}
          onError={event => {
            const {message} = event.nativeEvent;
            // eslint-disable-next-line no-alert
            alert(message);
          }}
          onCancelNavigation={() => onCancelNavigation && onCancelNavigation()}
          onArrive={() => {
            // eslint-disable-next-line no-alert
            //alert('You have reached your destination');
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
