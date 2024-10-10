# MOVE SDK
The MOVE SDK enables you to collect location data, motion information and other sensor data from your users smartphones. This data is then transmitted to our backend, where it is evaluated, enriched with industry leading machine learning algorithms and applied to a comprehensive 24/7 timeline.
This timeline gives you insights on where your users move and how they get there, be it in the past, the present or even the future (via our prediction algorithms). With our MOVE SDK, you will gain a deep understanding of your users personas and service them in a whole new way, completely transforming the way you interact with them.

The MOVE SDK Sample App shows you how to integrate our SDK very easily into your existing or future app.

## To run this project:
1. Request a PRODUCT ID and the MOVE_API_KEY by contacting Dolphin MOVE.
2. This sample uses the Gradle build system. To build this project, use the "gradlew build" command or use "Import Project" in Android Studio.
3. To add it to your project add the following lines to your project build.gradle / allprojects / repositories:
        maven {
            url "https://dolphin.jfrog.io/artifactory/move-sdk-libs-release"
        }
4. In the app module build.gradle file please replace the value "1234" (MOVE_API_PROJECT) with the PROJECT ID you received. You can find this also in the [MOVE Dashboard](https://dashboard.movesdk.com/admin/sdkConfig).
5. In the same app module build.gradle file please replace the value "\"YOUR_API_KEY\"" (MOVE_API_KEY) with the API Key you received. You can find this also in the [MOVE Dashboard](https://dashboard.movesdk.com/admin/sdkConfig/keys).
6. Clean, build and run the application on your device.

Reference: [MOVE Android SDK documentation](https://docs.movesdk.com/)

## Starting Point
MoveSampleApplication and MoveSdkManager are recommended starting points for embedding the SDK in an already existing or new project.

### Authorization
After contacting us and getting a PROJECT ID, use it to fetch an authentication code from the Move Server (see fun registerUserWithAuthCode(..) / MoveBackendApi). This authentication code will be passed to the SDK on initialization (see fun configureMoveSdk(authCode: String) / MoveSdkManager) and will be used by the SDK to authenticate its services.

With authCodeListener (MoveSdkManager) you can listen to the status of the authentication code.

### Configuration
Within configureMoveSdk(authCode: String) / MoveSdkManager) you can see an example of how to configure the MOVE SDK. You can set the desired configurations for the MOVE SDK here.

All permissions required for the requested configurations must be granted. MoveSdk.StateListener (see MoveSdkManager) will be triggered otherwise.

### About the MOVE SDK Sample App
In MoveBackendApi you find one possibility to register your project to our backend. To request the necessary data from our backend, we use the retrofit API in our sample app (see registerUser(..) / MoveBackendApi). Furthermore, you will find a way to request the permissions necessary for the MOVE SDK.

After the credentials have been requested and the app permissions have been obtained from the user, the MOVE SDK will be initialized (see MoveSdkManager).

With MoveSdk.StateListener / onStateChanged(...) (see MoveSdkManager) it is possible to get the state of the MOVE SDK and visualize it to the user.

MoveSdk.TripStateListener / onTripStateChanged(...) (see MoveSdkManager) is used to get the trip state changes from the MOVE SDK. For a better usability you can present these information to the user.

In MoveSampleActivity you can also find an example how to implement different notifications for the user. You should adapt these examples to your requirements or use your own implementation (see createRecognitionNotification(...) and createDrivingNotification(...) / MoveSdkManager).

The visual representation for this sample app of the app permissions and MOVE SDK states can be found in MoveSampleActivity, MoveSampleViewModel and MoveSampleFragment.

### Attention
This sample app uses the [RETROFIT API](https://square.github.io/retrofit/) to request the necessary credentials. However, you can use any other HTTPS API for this purpose.

## Screenshots
If the registration was successful and all necessary permissions are granted you should see the following -> see MOVE_SDK_disabled.png
To enable the MOVE SDK you can use the switch to turn the MOVE SDK on / off -> see MOVE_SDK_enabled.png

## Support
info@dolph.in

## License
The contents of this repository are licensed under the
[Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

