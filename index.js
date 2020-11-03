import PropTypes from 'prop-types';
import React from 'react';
import {
  Platform,
  requireNativeComponent,
  StyleSheet,
  View,
} from 'react-native';
import { useInterval } from './hooks/useInterval';

// This wrapper component is needed to fix an android quirk with RN and native views.
// You can see https://github.com/facebook/react-native/issues/17968 and
// https://github.com/mapbox/mapbox-navigation-android/issues/3050#issuecomment-720172578
// for context. If you know how to fix this on the native side please submit a PR!
const WrapperComponent = (props) => {
  const [ghostViewHeight, setGhostViewHeight] = React.useState(1);

  if (Platform.OS === 'android') {
    useInterval(() => {
      setGhostViewHeight(ghostViewHeight === 1 ? 1.2 : 1);
    }, 1000);
  }

  return (
    <View style={styles.flexIt}>
      <MapboxNavigation {...props} />
      <View style={{ height: ghostViewHeight }} />
    </View>
  );
};

const MapboxNavigation = (props) => {
  return <RNMapboxNavigation style={styles.flexIt} {...props} />;
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

const styles = StyleSheet.create({
  flexIt: {
    flex: 1,
  },
});

export default WrapperComponent;
