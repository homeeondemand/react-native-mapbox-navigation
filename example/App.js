/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React, {useEffect} from 'react';
import {SafeAreaView, useColorScheme} from 'react-native';
import {Colors} from 'react-native/Libraries/NewAppScreen';
import NavigationComponent from './NavigationComponent';
import {PermissionsAndroid} from 'react-native';

const App = () => {
  const isDarkMode = useColorScheme() === 'dark';

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
    flex: 1,
  };

  useEffect(() => {
    const requestLocationPermission = async () => {
      try {
        await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          {
            title: 'Example App',
            message: 'Example App access to your location ',
          },
        );
      } catch (err) {
        console.warn(err);
      }
    };

    requestLocationPermission();
  }, []);

  // const abc = [
  //   DirectionsRoute{
  //     routeIndex=0, 
  //     distance=1634.384, 
  //     duration=214.27, 
  //     durationTypical=null, 
  //     geometry="eqwyjAjwgpgED|lB@jHyRFgH@wZFqC@oB@k]HkC@}@?yHDQrJA`D?pK?tg@bAvo@b@lI`AjLZxD@tH@`L@tEBhVDx\Dz^Dx^?dE@`IFld@?dC@hHH~y@@fHRtjBBzK^ppDPr|Ai@f\KjIGvASrAWnAc@tAq@t@e@dASbAGjAFdAP`A\|@h@t@t@d@x@R|@?v@Sr@c@h@o@\u@TiAFmAGkASeA[u@c@o@m@c@s@U{@GQB,weight=251.777,weightName=auto", 
  //     legs=[
  //       RouteLeg{
  //         distance=1634.384, 
  //         duration=214.27, 
  //         durationTypical=null, 
  //         summary=Youngfield Street, West 32nd Avenue, 
  //         admins=[
  //           Admin{countryCode=US, countryCodeAlpha3=USA}], 
  //           steps=[
  //             LegStep{
  //               distance=164.0, 
  //               duration=37.807, 
  //               durationTypical=null, 
  //               speedLimitUnit=mph, 
  //               speedLimitSign=mutcd, 
  //               geometry=eqwyjAjwgpgED|lB@jH, 
  //               ref=null, 
  //               destinations=null, 
  //               mode=driving, 
  //               pronunciation=null, 
  //               rotaryName=null, 
  //               rotaryPronunciation=null, 
  //               maneuver=StepManeuver{
  //                 rawLocation=[-105.140614, 39.760163], 
  //                 bearingBefore=0.0, 
  //                 bearingAfter=270.0, 
  //                 instruction=Drive west on West 31st Avenue., 
  //                 type=depart, 
  //                 modifier=null, 
  //                 exit=null
  //               }, 
  //               voiceInstructions=[
  //                 VoiceInstructions{
  //                   distanceAlongGeometry=164.0, 
  //                   announcement=Drive west on West 31st Avenue. Then, in 500 feet, Turn right onto Youngfield Street., 
  //                   ssmlAnnouncement=<speak><amazon:effect name="drc"><prosody rate="1.08">Drive west on <say-as interpret-as="address">West 31st Avenue</say-as>. Then, in 500 feet, Turn right onto <say-as interpret-as="address">Youngfield Street</say-as>.</prosody></amazon:effect></speak>
  //                 }, 
  //                 VoiceInstructions{
  //                   distanceAlongGeometry=50.0, 
  //                   announcement=Turn right onto Youngfield Street., ssmlAnnouncement=<speak><amazon:effect name="drc"><prosody rate="1.08">Turn right onto <say-as interpret-as="address">Youngfield Street</say-as>.</prosody></amazon:effect></speak>
  //                 }
  //               ], 
  //               bannerInstructions=[
  //                 BannerInstructions{
  //                   distanceAlongGeometry=164.0, 
  //                   primary=BannerText{
  //                     text=Youngfield Street, 
  //                     components=[
  //                       BannerComponents{
  //                         text=Youngfield Street, 
  //                         type=text, 
  //                         subType=null, 
  //                         abbreviation=null, 
  //                         abbreviationPriority=null, 
  //                         imageBaseUrl=null, 
  //                         imageUrl=null, 
  //                         directions=null, 
  //                         active=null, 
  //                         activeDirection=null
  //                       }
  //                     ], 
  //                     type=turn, 
  //                     modifier=right, 
  //                     degrees=null, 
  //                     drivingSide=null
  //                   }, 
  //                   secondary=null, 
  //                   sub=null, 
  //                   view=null
  //                 }
  //               ], 
  //               drivingSide=right, 
  //               weight=41.135, 
  //               intersections=[
  //                 StepIntersection{
  //                   rawLocation=[-105.140614, 39.760163], 
  //                   bearings=[270], 
  //                   classes=null, 
  //                   entry=[true], 
  //                   in=null, 
  //                   out=0, 
  //                   lanes=null, 
  //                   geometryIndex=0, 
  //                   isUrban=true, 
  //                   adminIndex=0, 
  //                   restStop=null, 
  //                   tollCollection=null, 
  //                   mapboxStreetsV8=MapboxStreetsV8{
  //                     roadClass=street
  //                   }, 
  //                   tunnelName=null
  //                 }, 
  //                 StepIntersection{
  //                   rawLocation=[-105.142373, 39.76016], 
  //                   bearings=[0, 90, 270], 
  //                   classes=null, 
  //                   entry=[true, false, true], 
  //                   in=1, 
  //                   out=2, 
  //                   lanes=null, 
  //                   geometryIndex=1, 
  //                   isUrban=true, 
  //                   adminIndex=0, 
  //                   restStop=null, 
  //                   tollCollection=null, 
  //                   mapboxStreetsV8=MapboxStreetsV8{
  //                     roadClass=street
  //                   }, 
  //                   tunnelName=null
  //                 }
  //               ], 
  //               exits=null
  //             }
  //           ]
  //         ]
  //       }
  //     ]
  //   }
  // ]


  //   const xyz = [
  //     DirectionsRoute{
  //       routeIndex=null, 
  //       distance=9719.919, 
  //       duration=1996.479, 
  //       durationTypical=null, 
  //       geometry=kntvCugixKKaA~A`@tCt@?ZAr@@\B\FTj@j@|A`BDDVTx@x@JP@DBPBZ?ZInAu@fLAdAExBMz@zA`Eo@bCCHDL?t@BX]R[LEBsEhBsA\kAZ{Cv@]LqCbA_Cl@gCt@y@TwAb@KBgF`BMDaAXoDfAyAXwA\gHtBgCp@]Fa@AUKSMMWIYEY?]D[JUNSZSNc@Hg@A[Kw@]sAK}@UuASuAEy@Cc@AM]uAKOAh@@hADl@LlAb@bCsACUeASqAImA{GMkCG@e@@eAIAGCGECGEM?M@MBGDEBEFCDAFABsB@oA?mABuAxKHrA@@mA`@Q@aDCwIIo@c@gEYyBe@BS_BM_ASaBWuBOmASwAS_BUsAU{AU{Am@eDa@sB[{AWsAo@LwFbAgF|@wNnCsKnB_@yB_@}B_@yB[yBgEx@yHrAUFMBGAGCEECECGAG?G@GFORa@eBsAmBmA}B_BaC}AqCkBQCMFO?MEKIEKAIAIKUOMc@]wCmBw@k@uBwAqBoA?AE@G?CACCCE?GQOu@c@s@]iAe@}Ag@uC{@}EiAkCi@w@Mk@K[ASBq@JcARaBb@kBl@k@RAPILIDI?GCGECEAI?G?EBE@EDCeAyBO_@o@mAWi@e@gA[u@a@{@EKoA_CMY]_@SUE?GEEGAKBIDEDAp@a@\[~@w@_BiBa@X?N`AvB~@w@j@k@dAy@x@q@t@m@NR`AbA`A~@gAbAcAx@, 
  //       weight=2984.588, 
  //       weightName=auto, 
  //       legs=[
  //         RouteLeg{
  //           distance=9719.919, 
  //           duration=1996.479, 
  //           durationTypical=null, 
  //           summary=Mehmoodabad Main Road, کورنگی روڈ, 
  //           admins=[
  //             Admin{
  //               countryCode=PK, 
  //               countryCodeAlpha3=PAK
  //             }
  //           ], 
  //           steps=[
  //             LegStep{
  //               distance=33.765, 
  //               duration=4.052, 
  //               durationTypical=null, 
  //               speedLimitUnit=null, 
  //               speedLimitSign=null, 
  //               geometry=kntvCugixKKaA, 
  //               name=, 
  //               ref=null, 
  //               destinations=null, 
  //               mode=driving, 
  //               pronunciation=null, 
  //               rotaryName=null, 
  //               rotaryPronunciation=null, 
  //               maneuver=StepManeuver{
  //                 rawLocation=[67.063154, 24.849818], 
  //                 bearingBefore=0.0, 
  //                 bearingAfter=78.0, 
  //                 instruction=Drive east., 
  //                 type=depart, 
  //                 modifier=null, 
  //                 exit=null
  //               }, 
  //               voiceInstructions=null, 
  //               bannerInstructions=null, 
  //               drivingSide=left, 
  //               weight=4.964, 
  //               intersections=[
  //                 StepIntersection{
  //                   rawLocation=[67.063154, 24.849818], 
  //                   bearings=[78], 
  //                   classes=null, 
  //                   entry=[true], 
  //                   in=null, 
  //                   out=0, 
  //                   lanes=null, 
  //                   geometryIndex=0, 
  //                   isUrban=true, 
  //                   adminIndex=0, 
  //                   restStop=null, 
  //                   tollCollection=null, 
  //                   mapboxStreetsV8=MapboxStreetsV8{roadClass=street}, 
  //                   tunnelName=null
  //                 }
  //               ], 
  //               exits=null
  //             }, 
  //             LegStep{
  //               distance=144.0, 
  //               duration=50.464, 
  //               durationTypical=null, 
  //               speedLimitUnit=null, 
  //               speedLimitSign=null, 
  //               geometry=wntvCwiixK~A`@tCt@, 
  //               name=, 
  //               ref=null, 
  //               destinations=null, 
  //               mode=driving, 
  //               pronunciation=null, 
  //               rotaryName=null, 
  //               rotaryPronunciation=null, 
  //               maneuver=StepManeuver{
  //                 rawLocation=[67.063482, 24.84988], 
  //                 bearingBefore=78.0, 
  //                 bearingAfter=198.0, 
  //                 instruction=Turn right., 
  //                 type=turn, 
  //                 modifier=right, 
  //                 exit=null
  //               }, 
  //               voiceInstructions=null, 
  //               bannerInstructions=null, 
  //               drivingSide=left, 
  //               weight=79.67, 
  //               intersections=[
  //                 StepIntersection{
  //                   rawLocation=[67.063482, 24.84988], 
  //                   bearings=[18, 84, 198, 258], 
  //                   classes=null, 
  //                   entry=[true, true, true, false], 
  //                   in=3, 
  //                   out=2, 
  //                   lanes=null, 
  //                   geometryIndex=1, 
  //                   isUrban=true, 
  //                   adminIndex=0, 
  //                   restStop=null, 
  //                   tollCollection=null, 
  //                   mapboxStreetsV8=MapboxStreetsV8{roadClass=street}, 
  //                   tunnelName=null
  //                 }, 
  //                 StepIntersection{
  //                   rawLocation=[67.063314, 24.849403], 
  //                   bearings=[18, 75, 198], 
  //                   classes=null, 
  //                   entry=[false, true, true], 
  //                   in=0, 
  //                   out=2, 
  //                   lanes=null, 
  //                   geometryIndex=2, 
  //                   isUrban=true, 
  //                   adminIndex=0, 
  //                   restStop=null, 
  //                   tollCollection=null, 
  //                   mapboxStreetsV8=MapboxStreetsV8{roadClass=street}, tunnelName=null
  //                 }
  //               ], 
  //               exits=null
  //             }, 
  //             LegStep{
  //               distance=880.0, 
  //               duration=166.963, 
  //               durationTypical=null, 
  //               speedLimitUnit=null, 
  //               speedLimitSign=null, 
  //               geometry=agtvC_gixK?ZAr@@\B\FTj@j@|A`BDDVTx@x@JP@DBPBZ?ZInAu@fLAdAExBMz@zA`Eo@bCCH, 
  //               name=Mehmoodabad Main Road, 
  //               ref=null, 
  //               destinations=null, 
  //               mode=driving, 
  //               pronunciation=null, 
  //               rotaryName=null, 
  //               rotaryPronunciation=null, 
  //               maneuver=StepManeuver{
  //                 rawLocation=[67.063038, 24.848651], 
  //                 bearingBefore=198.0, 
  //                 bearingAfter=271.0, 
  //                 instruction=Turn right onto Mehmoodabad Main Road., 
  //                 type=end of road, 
  //                 modifier=right, 
  //                 exit=null
  //               }, 
  //               voiceInstructions=null, 
  //               bannerInstructions=null, 
  //               drivingSide=left, 
  //               weight=220.446, 
  //               intersections=[
  //                 StepIntersection{
  //                   rawLocation=[67.063038, 24.848651], 
  //                   bearings=[18, 90, 271], 
  //                   classes=null, 
  //                   entry=[false, true, true], 
  //                   in=0, 
  //                   out=2, 
  //                   lanes=null, 
  //                   geometryIndex=3, 
  //                   isUrban=true, 
  //                   adminIndex=0, 
  //                   restStop=null, 
  //                   tollCollection=null, 
  //                   mapboxStreetsV8=MapboxStreetsV8{roadClass=tertiary}, 
  //                   tunnelName=null
  //                 }, 
  //                 StepIntersection{
  //                   rawLocation=[67.062896, 24.848653], 
  //                   bearings=[4, 91, 271], 
  //                   classes=null, 
  //                   entry=[true, false, true], 
  //                   in=1, 
  //                   out=2, 
  //                   lanes=null, 
  //                   geometryIndex=4, 
  //                   isUrban=true, 
  //                   adminIndex=0

  return (
    <SafeAreaView style={backgroundStyle}>
      <NavigationComponent
        origin={[67.0785205, 24.8872153]}
        destination={[67.07814, 24.885666]}
        customRoutes={[
          [67.0785205, 24.8872153],
          [67.063154, 24.849818],
          [67.063386, 24.849607],
          [67.063233, 24.849184],
          [67.060686, 24.847349],
          [67.05853, 24.847615],
          [67.056401, 24.847578],
          [67.054741, 24.847661],
          [67.053531, 24.850061],
          [67.052048, 24.853742],
          [67.051127, 24.856007],
          [67.050128, 24.858714],
          [67.053542, 24.859905],
          [67.059572, 24.859769],
          [67.065762, 24.86125],
          [67.067911, 24.863602],
          [67.066797, 24.867586],
          [67.066727, 24.869255],
          [67.068641, 24.87041],
          [67.068176, 24.872092],
          [67.069879, 24.874289],
          [67.071523, 24.87652],
          [67.073738, 24.879734],
          [67.074686, 24.884207],
          [67.075582, 24.88621],
          [67.077448, 24.887272],
          [67.078639, 24.886928],
          [67.078646, 24.886934],
          [67.078444, 24.887264],
          [67.07814, 24.885666],
        ]}
      />
    </SafeAreaView>
  );
};

export default App;
