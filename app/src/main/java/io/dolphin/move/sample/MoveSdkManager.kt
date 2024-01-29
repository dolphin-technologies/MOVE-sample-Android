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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import io.dolphin.move.*
import io.dolphin.move.sample.MoveSampleApplication.Companion.PREF_ACCESS_TOKEN
import io.dolphin.move.sample.MoveSampleApplication.Companion.PREF_REFRESH_TOKEN
import io.dolphin.move.sample.MoveSampleApplication.Companion.PREF_SHARED_NAME
import io.dolphin.move.sample.MoveSampleApplication.Companion.PREF_USER_ID
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

    private var moveAuth: MoveAuth? = null
    internal var moveSdk: MoveSdk? = null

    private val moveStateFlow = MutableStateFlow<MoveSdkState>(MoveSdkState.Uninitialised)
    private val moveAuthStateFlow = MutableStateFlow<MoveAuthState>(MoveAuthState.UNKNOWN)
    private val moveTripStateFlow = MutableStateFlow(MoveTripState.UNKNOWN)
    private val moveConfigErrorFlow = MutableStateFlow<MoveConfigurationError?>(null)
    private val moveErrorsFlow = MutableStateFlow<List<MoveServiceFailure>>(emptyList())
    private val moveWarningsFlow = MutableStateFlow<List<MoveServiceWarning>>(emptyList())
    private val assistanceStateFlow = MutableStateFlow<MoveAssistanceCallStatus?>(null)


    fun fetchMoveStateFlow(): StateFlow<MoveSdkState> {
        return moveStateFlow
    }

    fun fetchTripStateFlow(): StateFlow<MoveTripState> {
        return moveTripStateFlow
    }

    fun fetchConfigErrorFlow(): StateFlow<MoveConfigurationError?> {
        return moveConfigErrorFlow
    }

    fun fetchErrorsFlow(): StateFlow<List<MoveServiceFailure>> {
        return moveErrorsFlow
    }

    fun fetchWarningsFlow(): StateFlow<List<MoveServiceWarning>> {
        return moveWarningsFlow
    }

    fun fetchAssistanceStateFlow(): StateFlow<MoveAssistanceCallStatus?> {
        return assistanceStateFlow
    }

    private var initListener: MoveSdk.InitializeListener = object : MoveSdk.InitializeListener {

        // Triggers whenever an error during the MOVE SDK initialization occurs.
        override fun onError(error: MoveConfigurationError) {
            Log.e("MoveConfigurationError", error.toString())
            launch {
                moveConfigErrorFlow.emit(error)
            }
            when (error) {
                is MoveConfigurationError.ServiceUnreachable -> {
                    Log.e(
                        "MoveConfigurationError",
                        "The connection to our servers failed. Please ensure that you have a valid internet connection. If the problem still remains, please contact customer support"
                    )
                }

                else -> {
                    Log.e(
                        "MoveConfigurationError",
                        "An unknown error occurred."
                    )}
            }
        }
    }

    private var sdkStateListener: MoveSdk.StateListener = object : MoveSdk.StateListener {

        private var lastState: MoveSdkState = MoveSdkState.Uninitialised

        override fun onStateChanged(sdk: MoveSdk, state: MoveSdkState) {
            moveStateFlow.value = state
            // If we received ready, we directly go to running
            if (state == MoveSdkState.Ready &&
                lastState == MoveSdkState.Uninitialised
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
                is MoveAuthState.VALID -> {
                    // Authentication is valid. Latest MoveAuth provided.
                }
                is MoveAuthState.INVALID -> {
                    // Authentication is invalid.
                }
                is MoveAuthState.UNKNOWN -> {
                    // The SDK authorization state when SDK is uninitialized.
                }
                else -> {}
            }
        }
    }

    private val warningListener = object : MoveSdk.MoveWarningListener {
        override fun onMoveWarning(serviceWarnings: List<MoveServiceWarning>) {
            moveWarningsFlow.value = serviceWarnings
            serviceWarnings.forEach { moveServiceWarning ->
                moveServiceWarning.warnings.forEach { moveWarning ->
                    Log.w(
                        TAG,
                        "${moveServiceWarning.service?.name().orEmpty()} - ${moveWarning.name}"
                    )
                }
            }
        }
    }

    private val errorListener = object : MoveSdk.MoveErrorListener {
        override fun onMoveError(serviceFailures: List<MoveServiceFailure>) {
            moveErrorsFlow.value = serviceFailures
            serviceFailures.forEach { moveServiceFailure ->
                moveServiceFailure.reasons.forEach { reasons ->
                    Log.e(TAG, "${moveServiceFailure.service.name()} - ${reasons.name}")
                }
            }
        }
    }

    private val assistanceListener = object : MoveSdk.AssistanceStateListener {
        override fun onAssistanceStateChanged(assistanceState: MoveAssistanceCallStatus) {
            assistanceStateFlow.value = assistanceState
            Log.d("MoveAssistanceCallStatus", assistanceState.name)
        }
    }

    init {
        Log.i(TAG, "Running MOVE SDK version ${MoveSdk.version}")
    }

    @SuppressLint("MissingPermission")
    private fun configureMoveSdk(moveAuth: MoveAuth) {
        // Let's configure the MoveSdk and register all the listeners for your usage.
        // see -> MoveSdk Wiki / Initializing the MOVE SDK
        moveSdk = MoveSdk.init(context)
        val recognitionNotification = createRecognitionNotification(context)
        val drivingNotification = createDrivingNotification(context)

        val moveConfig = createMoveConfig()
        moveSdk?.run {
            recognitionNotification(recognitionNotification)
            tripNotification(drivingNotification) // If the device is on a trip you can show an other notification icon.,
            walkingLocationNotification(recognitionNotification) // notification for places and walking path
            sdkStateListener(sdkStateListener)
            tripStateListener(tripStateListener)
            authStateUpdateListener(authStateListener)
            initializationListener(initListener)
            setServiceWarningListener(warningListener)
            setServiceErrorListener(errorListener)
            consoleLogging(true)
            allowMockLocations(BuildConfig.DEBUG) // mock location not recommended for use in production
        }
        MoveSdk.setup(moveAuth, moveConfig, start = true)
    }

    private fun createMoveConfig(): MoveConfig {
        // MoveSdk services configuration
        val moveDetectionServices: MutableSet<MoveDetectionService> = mutableSetOf()

        val drivingServices: MutableSet<DrivingService> = mutableSetOf()
        val walkingServices: MutableSet<WalkingService> = mutableSetOf()
        //val otherServices: MutableSet<OtherService> = mutableSetOf()

        drivingServices.add(DrivingService.DrivingBehaviour)
        drivingServices.add(DrivingService.DistractionFreeDriving)

        walkingServices.add(WalkingService.Location)

        //otherServices.add(OtherService.PointsOfInterest)
        //otherServices.add(OtherService.Crash)
        //otherServices.add(OtherService.AssistanceCall)

        moveDetectionServices.add(MoveDetectionService.Driving(drivingServices = drivingServices.toList()))
        moveDetectionServices.add(MoveDetectionService.Walking(walkingServices = walkingServices.toList()))

        moveDetectionServices.add(MoveDetectionService.Cycling)
        moveDetectionServices.add(MoveDetectionService.Places)
        moveDetectionServices.add(MoveDetectionService.PublicTransport)
        moveDetectionServices.add(MoveDetectionService.PointsOfInterest)
        moveDetectionServices.add(MoveDetectionService.AutomaticImpactDetection)
        moveDetectionServices.add(MoveDetectionService.AssistanceCall)

        return MoveConfig(
            moveDetectionServices = moveDetectionServices.toList(),
        )
    }

    private fun createRecognitionNotification(context: Context): NotificationCompat.Builder {
        // Creates a notification for the user while detecting activities, trips and more.
        // see -> MoveSdk Wiki / Notification Management
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = NOTIFICATION_CHANNEL
        val name: CharSequence = context.getString(R.string.notification_recognition)
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel(channelId, name, importance)
        notificationManager.createNotificationChannel(mChannel)

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
        val name: CharSequence = context.getString(R.string.notification_driving)
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel(NOTIFICATION_CHANNEL, name, importance)
        notificationManager.createNotificationChannel(mChannel)

        val intentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else 0
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        var contentIntent: PendingIntent? = null
        if (intent != null) {
            contentIntent = PendingIntent.getActivity(context, 0, intent, intentFlags)
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
    @Deprecated(" Do that on your own backend, not on the frontend")
    fun registerUser(context: Context) {
        // Use the user ids of your customers.
        // This is only for the sample app. In real world it should be an unique userId.
        val userId = System.currentTimeMillis().toString()

        // Let's fetch the necessary values from the MOVE backend.
        // In this sample app we use retrofit.
        // see -> https://developer.android.com/training/basics/network-ops/connecting
        val apiInterface = MoveBackendApi.create().registerUser(
            registerRequest = RegisterRequest(userId),
            authHeader = "Bearer ${BuildConfig.MOVE_API_KEY}"
        )

        apiInterface.enqueue(object : Callback<RegisterResponse> {

            override fun onResponse(
                call: Call<RegisterResponse>,
                response: Response<RegisterResponse>
            ) {
                // Handle the response from the MOVE backend.
                val responseBody = response.body()
                if (responseBody != null) {
                    val projectId = responseBody.projectId.toLong()
                    val accessToken = responseBody.accessToken
                    val refreshToken = responseBody.refreshToken

                    if (projectId != BuildConfig.MOVE_API_PROJECT) {
                        throw IllegalArgumentException(
                            "ProjectID mismatch ($projectId vs ${BuildConfig.MOVE_API_PROJECT})" +
                                    ", please ensure that you are using the correct ProjectID and API Key"
                        )
                    }

                    val sharedPref: SharedPreferences = context.getSharedPreferences(
                        PREF_SHARED_NAME,
                        Context.MODE_PRIVATE
                    )
                    val editor = sharedPref.edit()
                    editor.putString(PREF_USER_ID, userId)
                    editor.putString(PREF_ACCESS_TOKEN, accessToken)
                    editor.putString(PREF_REFRESH_TOKEN, refreshToken)
                    editor.apply()

                    moveAuth = MoveAuth(
                        BuildConfig.MOVE_API_PROJECT,
                        userId,
                        accessToken,
                        refreshToken
                    ).also {
                        // if we have already a SDK instance, we can just update authentication
                        moveSdk?.updateAuth(it)
                        // otherwise we just start it from scratch
                            ?: initSdk(it)
                    }
                } else {
                    Toast.makeText(context, "Check MOVE_API_KEY!", Toast.LENGTH_LONG)
                        .show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                // Make sure that in case of an error the request is executed again.
                Log.e(TAG, t.message, t)
                launch {
                    // NOTE: This not a real MoveConfigError, but for simplicity we just use that for now
                    moveConfigErrorFlow.emit(MoveConfigurationError.ServiceUnreachable)
                }
            }
        })
    }

    fun setupSdk(context: Context, canRegister: Boolean = false) {
        val sharedPref = context.getSharedPreferences(PREF_SHARED_NAME, Context.MODE_PRIVATE)
        val userId = sharedPref.getString(PREF_USER_ID, "")
        val accessToken = sharedPref.getString(PREF_ACCESS_TOKEN, "")
        val refreshToken = sharedPref.getString(PREF_REFRESH_TOKEN, "")
        if (!userId.isNullOrEmpty() && !accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
            // We have all necessary data to initialize the MoveSdk.
            val moveAuth =
                MoveAuth(BuildConfig.MOVE_API_PROJECT, userId, accessToken, refreshToken)
            initSdk(moveAuth)
        } else if (canRegister) {
            // No SDK data available -> let's get the data from the backend
            registerUser(context)
        }
    }

    fun initSdk(moveAuth: MoveAuth? = null) {
        moveAuth?.let {
            // if there is no authentication data passed, it will end in an configuration error
            configureMoveSdk(moveAuth)
        }
    }

    fun callAssistance() {
        moveSdk?.initiateAssistanceCall(
            assistanceListener = assistanceListener,
        )
    }

    fun updateConfig() {
        moveSdk?.updateConfig(createMoveConfig())
    }
}
