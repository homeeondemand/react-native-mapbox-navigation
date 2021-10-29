import PropTypes from 'prop-types';
import * as React from 'react';
import { requireNativeComponent, StyleSheet, NativeModules } from 'react-native';

const MapboxNavigation = (props) => {
  return <RNMapboxNavigation style={styles.flex} {...props} />;
};

export const MapboxNavigationModule = NativeModules.MapboxNavigationManager

MapboxNavigation.propTypes = {
  origin: PropTypes.array.isRequired,
  destination: PropTypes.array,
  camera: PropTypes.object,
  shouldSimulateRoute: PropTypes.bool,
  onLocationChange: PropTypes.func,
  onRouteProgressChange: PropTypes.func,
  onError: PropTypes.func,
  onCancelNavigation: PropTypes.func,
  onArrive: PropTypes.func,
  showsEndOfRouteFeedback: PropTypes.bool,
};

const RNMapboxNavigation = requireNativeComponent(
  'MapboxNavigation',
  MapboxNavigation
);

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
});

export default MapboxNavigation;
