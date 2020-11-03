# React Native Mapbox Navigation

Smart Mapbox turn-by-turn routing based on real-time traffic for React Native. A navigation UI ready to drop into your application.

## Installation Requirements

Before installing the SDK, you will need to gather the appropriate credentials. The SDK requires two pieces of sensitive information from your Mapbox account. If you don't have a Mapbox account: [sign up](https://account.mapbox.com/auth/signup/) and navigate to your [Account page](https://account.mapbox.com/). You'll need:

- **A public access token**: From your account's [tokens page](https://account.mapbox.com/access-tokens/), you can either copy your _default public token_ or click the **Create a token** button to create a new public token.
- **A secret access token with the `Downloads:Read` scope**.

1. From your account's [tokens page](https://account.mapbox.com/access-tokens/), click the **Create a token** button.
1. From the token creation page, give your token a name and make sure the box next to the `Downloads:Read` scope is checked.
1. Click the **Create token** button at the bottom of the page to create your token.
1. The token you've created is a _secret token_, which means you will only have one opportunity to copy it somewhere secure.

## Installation

```
npm install @homee/react-native-mapbox-navigation
```

Read the iOS specific instructions below before running `pod install`.

### iOS Specific Instructions

Place your secret token in a `.netrc` file in your home directory that contains this:

```
machine api.mapbox.com
login mapbox
password <INSERT SECRET TOKEN>
```

Place your public token in your project's `Info.plist` and and add a `MGLMapboxAccessToken` key whose value is your public access token.

Now you are ready to run the install the cocoapod:

```
cd ios && pod install
```

For more information you can read the [docs provided by Mapbox](https://docs.mapbox.com/ios/navigation/overview/#configure-credentials).

### Android Specific Instructions

Place your secret token in your android app's top level `gradle.properties` file:

```
MAPBOX_DOWNLOADS_TOKEN=SECRET_TOKEN_HERE
```

Open up your _project-level_ `build.gradle` file. Declare the Mapbox Downloads API's `releases/maven` endpoint in the `repositories` block.

```gradle
allprojects {
    repositories {
        maven {
            url 'https://api.mapbox.com/downloads/v2/releases/maven'
            authentication {
                basic(BasicAuthentication)
            }
            credentials {
                // Do not change the username below.
                // This should always be `mapbox` (not your username).
                username = "mapbox"
                // Use the secret token you stored in gradle.properties as the password
                password = project.properties['MAPBOX_DOWNLOADS_TOKEN'] ?: ""
            }
        }
    }
}
```

Place your public token in your project's `android/app/src/main/AndroidManifest.xml`

```xml
<meta-data android:name="MAPBOX_ACCESS_TOKEN"
    android:value="PUBLIC_TOKEN_HERE" />
```

For more information you can read the [docs provided by Mapbox](https://docs.mapbox.com/android/navigation/overview/#configure-credentials).

## Usage

TODO

## Contributing

Contributions are very welcome. Please check out the [contributing document](CONTRIBUTING.md).

## License

This project is [MIT](LICENSE) licensed.
