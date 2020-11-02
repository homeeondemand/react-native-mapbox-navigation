import PropTypes from 'prop-types';
import React from 'react';
import { requireNativeComponent } from 'react-native';

const MapboxNavigation = (props) => {
  return <RNMapboxNavigation {...props} />;
};

MapboxNavigation.propTypes = {
  origin: PropTypes.array.isRequired,
  destination: PropTypes.array.isRequired,
  shouldSimulateRoute: PropTypes.bool,
  onProgressChange: PropTypes.func,
  onError: PropTypes.func,
};

const RNMapboxNavigation = requireNativeComponent(
  'MapboxNavigation',
  MapboxNavigation
);

export default MapboxNavigation;
