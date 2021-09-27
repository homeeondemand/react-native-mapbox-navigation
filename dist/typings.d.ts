/** @type {[number, number]}
 * Provide an array with longitude and latitude [$longitude, $latitude]
 */
declare type Coordinate = [number, number];
declare type OnLocationChangeEvent = {
    nativeEvent?: {
        latitude: number;
        longitude: number;
    };
};
declare type OnRouteProgressChangeEvent = {
    nativeEvent?: {
        distanceTraveled: number;
        durationRemaining: number;
        fractionTraveled: number;
        distanceRemaining: number;
    };
};
declare type OnErrorEvent = {
    nativeEvent?: {
        message?: string;
    };
};
export interface IMapboxNavigationProps {
    origin: Coordinate;
    destination: Coordinate;
    shouldSimulateRoute?: boolean;
    onLocationChange?: (event: OnLocationChangeEvent) => void;
    onRouteProgressChange?: (event: OnRouteProgressChangeEvent) => void;
    onError?: (event: OnErrorEvent) => void;
    onCancelNavigation?: () => void;
    onArrive?: () => void;
    showsEndOfRouteFeedback?: boolean;
    hideStatusView?: boolean;
    mute?: boolean;
}
export {};
