#import "React/RCTViewManager.h"

@interface RCT_EXTERN_MODULE(MapboxNavigationManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(onProgressChange, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onError, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onCancelNavigation, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onNavigationFinish, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(origin, NSArray)
RCT_EXPORT_VIEW_PROPERTY(destination, NSArray)
RCT_EXPORT_VIEW_PROPERTY(shouldSimulateRoute, BOOL)

@end
