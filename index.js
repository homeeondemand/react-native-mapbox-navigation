import PropTypes from 'prop-types';
import * as React from 'react';
import { requireNativeComponent, StyleSheet, View } from 'react-native';

const MapboxNavigation = (props) => {
  return <RNMapboxNavigation style={styles.flex} {...props} />;
};

MapboxNavigation.propTypes = {
  origin: PropTypes.array.isRequired,
  destination: PropTypes.array.isRequired,
  shouldSimulateRoute: PropTypes.bool,
  onProgressChange: PropTypes.func,
  onError: PropTypes.func,
  onCancelNavigation: PropTypes.func,
  onNavigationFinish: PropTypes.func,
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
