## HopOn Android App

Ride-sharing demo app with simulated backend, Maps, Places autocomplete, and Routes-based path rendering.

## Requirements

- Android Studio / Android SDK
- JDK 17+ (or Android Studio bundled JDK)

## API Keys

This project uses two separate Google API keys:

- `apiKey`: Maps SDK for Android + Places API (New)
- `routesApiKey`: Routes API (Directions) for path generation

Add both to `local.properties` (do not commit this file):

```
sdk.dir=/PATH/TO/ANDROID/SDK
apiKey=YOUR_ANDROID_MAPS_KEY
routesApiKey=YOUR_ROUTES_KEY
mockMode=true
```

`mockMode` controls frontend data source on the map screen:

- `true`: uses placeholder places and mock nearby cab data (no Places/API dependency for UI development)
- `false`: uses real Places autocomplete and simulator websocket flow

### Required APIs in Google Cloud Console

For `apiKey`:

- Maps SDK for Android
- Places API (New)

If you restrict this key to Android apps, add:

- Package name: `com.tritech.hopon`
- SHA-1 of your debug keystore

For `routesApiKey`:

- Routes API
- Billing enabled on the same project

This key should be **unrestricted** (or restricted by IP/referrer), because the simulator calls the Routes API over HTTPS without Android credential headers.

## Build and Run

```
./gradlew clean installDebug
```

If you see map tiles missing, confirm the Maps SDK for Android is enabled for `apiKey` and the package/SHA-1 restrictions match your debug build.
      "type": "tripStart"
    }
    ```       
   * Trip Path
    ```json
    {
      "type": "tripPath",
      "path": [
        {
          "lat": 28.438370000000003,
          "lng": 77.09944
        },
        {
          "lat": 28.438450000000003,
          "lng": 77.1006
        },
        {
          "lat": 28.438480000000002,
          "lng": 77.10095000000001
        }
      ]
    }
    ``` 
   * Trip End
    ```json
    {
      "type": "tripEnd"
    }
    ```          
* The server sending the error event to the client received in `onError(error: String)`:
   * Direction API Failed
    ```json
    {
      "type": "directionApiFailed",
      "error": "Unable to resolve host \"maps.googleapis.com\": No address associated with hostname"
    }
    ```
   * Routes Not Available
    ```json
    {
      "type": "routesNotAvailable"
    }
    ```  

### License
```
   Copyright (C) 2024 Amit Shekhar

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
