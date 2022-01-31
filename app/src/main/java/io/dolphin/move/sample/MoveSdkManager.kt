/*
 *  Copyright 2021 Dolphin Technologies GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http:*www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package io.dolphin.move.sample

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import io.dolphin.move.*
import io.dolphin.move.sample.MoveSampleApplication.Companion.PREF_ACCESS_TOKEN
import io.dolphin.move.sample.MoveSampleApplication.Companion.PREF_CONTRACT_ID
import io.dolphin.move.sample.MoveSampleApplication.Companion.PREF_REFRESH_TOKEN
import io.dolphin.move.sample.MoveSampleApplication.Companion.PREF_SHARED_NAME
import io.dolphin.move.sample.backend.MoveBackendApi
import io.dolphin.move.sample.backend.RegisterRequest
import io.dolphin.move.sample.backend.RegisterResponse
import io.dolphin.move.sample.thirdparty.SingletonHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val NOTIFICATION_CHANNEL = "TRIP_CHANNEL"

class MoveSdkManager private constructor(private val context: Context) : CoroutineScope {

    private val job = Job()
    override val coroutineContext = Dispatchers.IO + job

    companion object : SingletonHolder<MoveSdkManager, Context>(::MoveSdkManager) {
        private const val TAG = "MoveSdkManager"
    }

    init {
        Log.i(TAG, "Running MOVE SDK version ${MoveSdk.version}")
    }

    private val moveSdkBuilder: MoveSdk.Builder
    private var moveAuth: MoveAuth? = null
    internal var moveSdk: MoveSdk? = null

    private val moveStateFlow = MutableStateFlow<MoveSdkState>(MoveSdkState.Uninitialised)
    private val moveAuthStateFlow = MutableStateFlow<MoveAuthState>(MoveAuthState.UNKNOWN)
    private val moveTripStateFlow = MutableStateFlow(MoveTripState.UNKNOWN)
    private val moveConfigErrorFlow = MutableStateFlow<MoveConfigurationError?>(null)

    fun fetchMoveStateFlow(): StateFlow<MoveSdkState> {
        return moveStateFlow
    }

    fun fetchTripStateFlow(): StateFlow<MoveTripState> {
        return moveTripStateFlow
    }

    fun fetchConfigErrorFlow(): StateFlow<MoveConfigurationError?> {
        return moveConfigErrorFlow
    }

    private var initListener: MoveSdk.InitializeListener = object : MoveSdk.InitializeListener {

        // Triggers whenever an error during the MOVE SDK initialization occurs.
        override fun onError(error: MoveConfigurationError) {
            Log.e("MoveConfigurationError", error.toString())
            launch {
                moveConfigErrorFlow.emit(error)
            }
            when (error) {
                is MoveConfigurationError.AuthInvalid -> {
                    // It might happen that the retrieved token is already outdated, so we need to get a new one
                    registerUser()
                }
                is MoveConfigurationError.ConfigMismatch -> {
                    Log.e(
                        "MoveConfigurationError",
                        "It seems that you art trying to use a service which you are not allowed. Please contact customer support"
                    )
                }
                is MoveConfigurationError.ServiceUnreachable -> {
                    Log.e(
                        "MoveConfigurationError",
                        "The connection to our servers failed. Please ensure that you have a valid internet connection. If the problem still remains, please contact customer support"
                    )
                }
            }
        }
    }

    private var sdkStateListener: MoveSdk.StateListener = object : MoveSdk.StateListener {

        private var lastState: MoveSdkState = MoveSdkState.Uninitialised

        override fun onStateChanged(sdk: MoveSdk, state: MoveSdkState) {
            moveStateFlow.value = state
            if (state is MoveSdkState.Error) {
                val reason = state.reason
                Log.w("MoveSdkState", "MOVE SDK reported an error: ${reason::class.simpleName}")
            } else {
                Log.d("MoveSdkState", state::class.simpleName.toString())
            }

            // If we received ready, we directly go to running
            if (state == MoveSdkState.Ready &&
                lastState == MoveSdkState.Uninitialised
                || lastState is MoveSdkState.Error
            ) {
                moveSdk?.startAutomaticDetection()
            }
            lastState = state
        }
    }

    private val tripStateListener = object : MoveSdk.TripStateListener {
        override fun onTripStateChanged(tripState: MoveTripState) {
            Log.d("MoveTripState", tripState.toString())
            moveTripStateFlow.value = tripState
        }
    }

    private val authStateListener = object : MoveSdk.AuthStateUpdateListener {

        // Triggers whenever the MoveAuthState changes.
        override fun onAuthStateUpdate(state: MoveAuthState) {
            moveAuthStateFlow.value = state

            when (state) {
                is MoveAuthState.EXPIRED -> {
                    // Latest MoveAuth expired and the SDK can't refresh it.
                    // Requesting new Auth using the product's API Key and then passing it to the SDK.
                    registerUser()
                }
                is MoveAuthState.VALID -> {
                    // Authentication is valid. Latest MoveAuth provided.
                }
                is MoveAuthState.UNKNOWN -> {
                    // The SDK authorization state when SDK is uninitialized.
                }
            }
        }
    }

    init {
        moveSdkBuilder = buildMoveSdk()
    }

    @SuppressLint("MissingPermission")
    private fun buildMoveSdk(): MoveSdk.Builder {
        // Let's build the MoveSdk and register all the listeners for your usage.
        // see -> MoveSdk Wiki / Initializing the MOVE SDK
        val recognitionNotification = createRecognitionNotification(context)
        val drivingNotification = createDrivingNotification(context)
        return MoveSdk.Builder()
            .timelineDetectionService(
                TimelineDetectionService.DRIVING,
                TimelineDetectionService.WALKING,
                TimelineDetectionService.BICYCLE,
                TimelineDetectionService.PLACES,
                TimelineDetectionService.PUBLIC_TRANSPORT,
            )
            .drivingServices(DrivingService.DistractionFreeDriving, DrivingService.DrivingBehaviour)
            .otherServices(OtherService.PointsOfInterest)   // Enter this line to activate the POI service feature. Please see also the MOVE SDK Wiki.
            .recognitionNotification(recognitionNotification)
            .tripNotification(drivingNotification)    // If the device is on a trip you can show an other notification icon.,
            .walkingLocationNotification(recognitionNotification) // notification for places and walking path
            .sdkStateListener(sdkStateListener)
            .tripStateListener(tripStateListener)
            .authStateUpdateListener(authStateListener)
            .initializationListener(initListener)
            .allowMockLocations(BuildConfig.DEBUG)    // mock location not recommended for use in production
    }

    private fun createRecognitionNotification(context: Context): NotificationCompat.Builder {
        // Creates a notification for the user while detecting activities, trips and more.
        // see -> MoveSdk Wiki / Notification Management
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = NOTIFICATION_CHANNEL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = context.getString(R.string.notification_recognition)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(channelId, name, importance)
            notificationManager.createNotificationChannel(mChannel)
        }

        val contentTitle = context.getString(R.string.waiting_for_trip)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(contentTitle)
            .setChannelId(channelId)
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        builder.setWhen(System.currentTimeMillis())
        builder.setShowWhen(true)
        builder.priority = NotificationCompat.PRIORITY_MIN
        builder.setOngoing(true)
        return builder
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createDrivingNotification(context: Context): NotificationCompat.Builder {
        // Creates a notification for the user during a drive.
        // see -> MoveSdk Wiki / Notification Management
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = context.getString(R.string.notification_driving)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(NOTIFICATION_CHANNEL, name, importance)
            notificationManager.createNotificationChannel(mChannel)
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        var contentIntent: PendingIntent? = null
        if (intent != null) {
            contentIntent = PendingIntent.getActivity(context, 0, intent, 0)
        }

        val contentTitle = context.getString(R.string.trip_active)
        val channelId = NOTIFICATION_CHANNEL
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(contentTitle)
            .setChannelId(channelId)
        if (contentIntent != null) { // unit test may have null values
            builder.setContentIntent(contentIntent)
        }
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        builder.setShowWhen(true)
        builder.priority = NotificationCompat.PRIORITY_MIN
        builder.setOngoing(true)
        return builder
    }

    /**
     * Register
     */
    @Deprecated(" Do that on your own backend, not on the fronted")
    fun registerUser() {
        // Use the contract ids of your customers.
        // This is only for the sample app. In real world it should be an unique userId.
        val contractId = System.currentTimeMillis().toString()

        // Let's fetch the necessary values from the MOVE backend.
        // In this sample app we use retrofit.
        // see -> https://developer.android.com/training/basics/network-ops/connecting
        val apiInterface = MoveBackendApi.create().registerUser(
            registerRequest = RegisterRequest(contractId),
            authHeader = "Bearer ${BuildConfig.MOVE_API_KEY}"
        )

        apiInterface.enqueue(object : Callback<RegisterResponse> {

            override fun onResponse(
                call: Call<RegisterResponse>?,
                response: Response<RegisterResponse>?
            ) {
                // Handle the response from the MOVE backend.
                response?.body()?.let { responseBody ->
                    val productId = responseBody.productId.toLong()
                    val accessToken = responseBody.accessToken
                    val refreshToken = responseBody.refreshToken
                    val contract = responseBody.contractId

                    if (productId != BuildConfig.MOVE_API_PRODUCT) {
                        throw IllegalArgumentException(
                            "ProductID mismatch ($productId vs ${BuildConfig.MOVE_API_PRODUCT})" +
                                    ", please ensure that you are using the correct ProductID and API Key"
                        )
                    }

                    val sharedPref: SharedPreferences = context.getSharedPreferences(
                        PREF_SHARED_NAME,
                        Context.MODE_PRIVATE
                    )
                    val editor = sharedPref.edit()
                    editor.putString(PREF_CONTRACT_ID, contract)
                    editor.putString(PREF_ACCESS_TOKEN, accessToken)
                    editor.putString(PREF_REFRESH_TOKEN, refreshToken)
                    editor.apply()

                    moveAuth = MoveAuth(BuildConfig.MOVE_API_PRODUCT, contractId, accessToken, refreshToken).also {
                        // if we have already a SDK instance, we can just update authentication
                        moveSdk?.updateAuth(it)
                        // otherwise we just start it from scratch
                            ?: initSdk(it)
                    }
                }
            }

            override fun onFailure(call: Call<RegisterResponse>?, t: Throwable?) {
                // Make sure that in case of an error the request is executed again.
                Log.e(TAG, t?.message, t)
                launch {
                    // NOTE: This not a real MoveConfigError, but for simplicity we just use that for now
                    moveConfigErrorFlow.emit(MoveConfigurationError.ServiceUnreachable)
                }
            }
        })
    }

    fun setupSdk(context: Context, canRegister: Boolean = false) {
        val sharedPref = context.getSharedPreferences(PREF_SHARED_NAME, Context.MODE_PRIVATE)
        val contractId = sharedPref.getString(PREF_CONTRACT_ID, "")
        val accessToken = sharedPref.getString(PREF_ACCESS_TOKEN, "")
        val refreshToken = sharedPref.getString(PREF_REFRESH_TOKEN, "")
        if (!contractId.isNullOrEmpty() && !accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
            // We have all necessary data to initialize the MoveSdk.
            val moveAuth = MoveAuth(BuildConfig.MOVE_API_PRODUCT, contractId, accessToken, refreshToken)
            initSdk(moveAuth)
        } else if (canRegister) {
            // No SDK data available -> let's get the data from the backend
            registerUser()
        }
    }

    fun initSdk(moveAuth: MoveAuth? = null) {
        moveAuth?.let {
            // if there is no authentication data passed, it will end in an configuration error
            moveSdkBuilder.authentication(moveAuth)
        }
        moveSdk = moveSdkBuilder.init(context)
    }
}