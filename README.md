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
5. In the same app module build.gradle file please replace the value "YOUR_API_KEY" (MOVE_API_KEY) with the API Key you received. You can find this also in the [MOVE Dashboard](https://dashboard.movesdk.com/admin/sdkConfig/keys).
6. Clean, build and run the application on your device.

Reference: [MOVE Android SDK documentation](https://docs.movesdk.com/)

## Starting Point
MoveSampleApplication and MoveSdkManager are recommended starting points for embedding the SDK in an already existing or new project.

### Authorization
After contacting us and getting a PROJECT ID, use it to fetch a MoveAuth from the Move Server (see fun registerUser(..) / MoveBackendApi). MoveAuth object will be passed to the SDK on initialization (see fun configureMoveSdk(moveAuth: MoveAuth) / MoveSdkManager) and will be used by the SDK to authenticate its services.

If the provided MoveAuth was invalid, the MOVE SDK should be shutdown (see MoveSdk.AuthStateUpdateListener / see MoveSdkManager). Check Initialization for more details about possible outcomes.

### Configuration
With MoveSdk.Builder (see fun configureMoveSdk(moveAuth: MoveAuth) / MoveSdkManager) you are able to configure which of the licensed Move services should be enabled.

All permissions required for the requested configurations must be granted. MoveSdk.StateListener (see MoveSdkManager) will be triggered otherwise.

### About the MOVE SDK Sample App
In MoveBackendApi you find one possibility to register your project to our backend. To request the necessary data from our backend, we use the retrofit API in our sample app (see registerUser(..) / MoveBackendApi). Furthermore, you will find a way to request the permissions necessary for the MOVE SDK.

After the credentials have been requested and the app permissions have been obtained from the user, the MOVE SDK will be initialized (see fun buildMoveSdk(moveAuth: MoveAuth) / MoveSdkManager).

MoveSdk.AuthStateUpdateListener (see MoveSdkManager) is used to inform about the status of the authentication credentials.

With MoveSdk.StateListener / onStateChanged(...) (see MoveSdkManager) it is possible to get the state of the MOVE SDK and visualize it to the user.

MoveSdk.TripStateListener / onTripStateChanged(...) (see MoveSdkManager) is used to get the trip state changes from the MOVE SDK. For a better usability you can present these information to the user.

In MoveSampleActivity you can also find an example how to implement different notifications for the user. You should adapt these examples to your requirements or use your own implementation (see createRecognitionNotification(...) and createDrivingNotification(...) / MoveSdkManager).

The visual representation for this sample app of the app permissions and MOVE SDK states can be found in MoveSampleActivity, MoveSampleViewModel and MoveSampleFragment.

### Attention
In the MOVE SDK Sample app at the time of initialization (see fun buildMoveSdk(moveAuth: MoveAuth) / MoveSdkManager) you can find the line .allowMockLocations(BuildConfig.DEBUG). You should only use this to debug your app with mock locations and remove it in production releases.

This sample app uses the [RETROFIT API](https://square.github.io/retrofit/) to request the necessary credentials. However, you can use any other HTTPS API for this purpose.

## Screenshots
If the registration was successful and all necessary permissions are granted you should see the following -> see MOVE_SDK_disabled.png
To enable the MOVE SDK you can use the switch to turn the MOVE SDK on / off -> see MOVE_SDK_enabled.png

## Support
info@dolph.in

## Pre-requisites
- Android Studio Arctic Fox | 2020.3.1 Patch 2
- Android Gradle Plugin Version 4.1.3
- Gradle Version 6.5
- Kotlin Version 1.5.30
- Compile Sdk Version 31
- Android Build Tools 30.0.2

## Dependencies
### App
- androidx.appcompat:appcompat:1.3.1
- com.squareup.retrofit2:converter-gson:2.9.0
- com.google.android.material:material:1.4.0
- io.dolphin.move:move-sdk:1.3.0.6
- com.squareup.retrofit2:retrofit:2.9.0

### Android Studio
- androidx.activity:activity:1.2.4
- androidx.annotation:annotation:1.2.0
- androidx.annotation:annotation-experimental
- androidx.appcompat:appcompat:1.3.1
- androidx.appcompat:appcompat-resources:1.3.1
- androidx.arch.core:core-common:2.1.0
- androidx.arch.core:core-runtime:2.1.0
- androidx.cardview:cardview:1.0.0
- androidx.collection:collection:1.1.0
- androidx.constraintlayout:constraintlayout:2.0.1
- androidx.constraintlayout:constraintlayout-solver:2.0.1
- androidx.coordinatorlayout:coordinatorlayout:1.1.0
- androidx.core:core
- androidx.core:core-ktx
- androidx.cursoradapter:cursoradapter:1.0.0
- androidx.customview:customview:1.0.0
- androidx.documentfile:documentfile:1.0.0
- androidx.drawerlayout:drawerlayout:1.0.0
- androidx.fragment:fragment:1.3.6
- androidx.interpolator:interpolator:1.0.0
- androidx.legacy:legacy-support-core-utils:1.0.0
- androidx.lifecycle:lifecycle-common:2.3.1
- androidx.lifecycle:lifecycle-livedata:2.1.0
- androidx.lifecycle:lifecycle-livedata-core:2.3.1
- androidx.lifecycle:lifecycle-runtime:2.3.1
- androidx.lifecycle:lifecycle-viewmodel:2.3.1
- androidx.lifecycle:lifecycle-viewmodel-savedstate:2.3.1
- androidx.loader:loader:1.0.0
- androidx.localbroadcastmanager:localbroadcastmanager:1.0.0
- androidx.print:print:1.0.0
- androidx.recyclerview:recyclerview:1.1.0
- androidx.room:room-common:2.3.0
- androidx.room:room-runtime:2.3.0
- androidx.savedstate:savedstate:1.1.0
- androidx.sqlite:sqlite:2.1.0
- androidx.sqlite:sqlite-framework:2.1.0
- androidx.transition:transition:1.2.0
- androidx.vectordrawable:vectordrawable:1.1.0
- androidx.vectordrawable:vectordrawable-animated:1.1.0
- androidx.versionedparcelable:versionedparcelable:1.1.1
- androidx.viewpager2:viewpager2:1.0.0
- androidx.viewpager:viewpager:1.0.0
- androidx.work:work-gcm:2.5.0
- androidx.work:work-runtime:2.5.0
- androidx.work:work-runtime-ktx:2.5.0
- com.google.android.gms:play-services-base:17.5.0
- com.google.android.gms:play-services-basement:17.5.0
- com.google.android.gms:play-services-fitness:20.0.0
- com.google.android.gms:play-services-location:18.0.0
- com.google.android.gms:play-services-places-placereport:17.0.0
- com.google.android.gms:play-services-tasks:17.2.0
- com.google.android.material:material:1.4.0
- com.google.code.gson:gson:2.8.6
- com.google.guava:listenablefuture:1.0
- com.squareup.moshi:moshi:1.8.0
- com.squareup.moshi:moshi-adapters:1.8.0
- com.squareup.moshi:moshi-kotlin:1.8.0
- com.squareup.okhttp3:logging-interceptor:3.8.0
- com.squareup.okhttp3:okhttp:3.14.9
- com.squareup.okio:okio:1.17.2
- com.squareup.retrofit2:converter-gson:2.9.0
- com.squareup.retrofit2:retrofit:2.9.0
- io.dolphin.move:move-sdk:1.3.0.6
- io.swagger:swagger-annotations:1.5.22
- joda-time:joda-time:2.9.9
- net.zetetic:android-database-sqlcipher:4.3.0
- org.jetbrains.kotlin:kotlin-reflect:1.4.32
- org.jetbrains.kotlin:kotlin-stdlib
- org.jetbrains.kotlin:kotlin-stdlib-common
- org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.1
- org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.4.1
- org.jetbrains:annotations:13.0

## License

The contents of this repository are licensed under the
[Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

