import PropTypes from 'prop-types';
import React from 'react';
import {
  Platform,
  requireNativeComponent,
  StyleSheet,
  View,
} from 'react-native';

const MapboxNavigation = (props) => {
  return <RNMapboxNavigation style={styles.flexIt} {...props} />;
};

MapboxNavigation.propTypes = {
  origin: PropTypes.array.isRequired,
  destination: PropTypes.array.isRequired,
  shouldSimulateRoute: PropTypes.bool,
  onProgressChange: PropTypes.func,
  onError: PropTypes.func,
  onCancelNavigation: PropTypes.func,
};

const RNMapboxNavigation = requireNativeComponent(
  'MapboxNavigation',
  MapboxNavigation
);

const styles = StyleSheet.create({
  flexIt: {
    flex: 1,
  },
});

export default MapboxNavigation;
