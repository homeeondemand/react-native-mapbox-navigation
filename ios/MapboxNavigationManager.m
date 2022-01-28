#import "React/RCTViewManager.h"
#import "React/RCTBridgeModule.h"

@interface RCT_EXTERN_MODULE(MapboxNavigationManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(mapToken, NSString)
RCT_EXPORT_VIEW_PROPERTY(navigationToken, NSString)

RCT_EXPORT_VIEW_PROPERTY(onLocationChange, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onRouteProgressChange, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onError, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onCancelNavigation, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onNavigationStarted, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onArrive, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onTap, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onReroute, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onMapMove, RCTDirectEventBlock)

RCT_EXPORT_VIEW_PROPERTY(origin, NSArray)
RCT_EXPORT_VIEW_PROPERTY(camera, NSDictionary)
RCT_EXPORT_VIEW_PROPERTY(destinationMarker, NSDictionary)
RCT_EXPORT_VIEW_PROPERTY(userLocatorMap, NSDictionary)
RCT_EXPORT_VIEW_PROPERTY(userLocatorNavigation, NSDictionary)
RCT_EXPORT_VIEW_PROPERTY(destination, NSArray)
RCT_EXPORT_VIEW_PROPERTY(styleURL, NSString)
RCT_EXPORT_VIEW_PROPERTY(transportMode, NSString)
RCT_EXPORT_VIEW_PROPERTY(followUser, BOOL)
RCT_EXPORT_VIEW_PROPERTY(showUserLocation, BOOL)
RCT_EXPORT_VIEW_PROPERTY(shouldSimulateRoute, BOOL)
RCT_EXPORT_VIEW_PROPERTY(showsEndOfRouteFeedback, BOOL)
RCT_EXPORT_VIEW_PROPERTY(markers, NSArray)
RCT_EXPORT_VIEW_PROPERTY(polylines, NSArray)
RCT_EXPORT_VIEW_PROPERTY(useImperial, BOOL)

RCT_EXTERN_METHOD(startNavigation)
RCT_EXTERN_METHOD(stopNavigation)
RCT_EXTERN_METHOD(startTracking)
RCT_EXTERN_METHOD(stopTracking)
RCT_EXTERN_METHOD(setCamera:(NSDictionary *) camera)

RCT_EXPORT_METHOD(captureScreenshot:(RCTResponseSenderBlock)callback)
{
 RCTLogInfo(@"Not implemented on iOS");
}

@end
