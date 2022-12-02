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

package io.dolphin.move.sample.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.dolphin.move.AssistanceCallState
import io.dolphin.move.MoveConfigurationError
import io.dolphin.move.MoveSdkState
import io.dolphin.move.MoveTripState
import io.dolphin.move.sample.BuildConfig
import io.dolphin.move.sample.MoveSampleApplication.Companion.PREF_USER_ID
import io.dolphin.move.sample.MoveSampleApplication.Companion.PREF_ENABLED
import io.dolphin.move.sample.MoveSampleApplication.Companion.PREF_SHARED_NAME
import io.dolphin.move.sample.MoveSdkManager
import io.dolphin.move.sample.PERMISSIONS_REQUEST_CODE
import io.dolphin.move.sample.thirdparty.SingleLiveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

enum class ActivationState {
    NOT_RUNNING,
    ERROR,
    RUNNING
}

class MoveSampleViewModel : ViewModel(), CoroutineScope {

    private val job = Job()
    override val coroutineContext = Dispatchers.Main + job

    internal val moveEnabled: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    internal val moveSdkActivation: MediatorLiveData<ActivationState> =
        MediatorLiveData<ActivationState>()

    private val userId: MediatorLiveData<String> = MediatorLiveData<String>()
    private val sdkState: MutableLiveData<MoveSdkState> = MutableLiveData<MoveSdkState>()
    private val tripState: MutableLiveData<MoveTripState> = MutableLiveData<MoveTripState>()

    internal val locationPermission: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    internal val backgroundPermission: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    internal val activityPermission: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    internal val phoneStatePermission: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    internal val overlayPermission: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    internal val batteryPermission: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    internal val bluetoothPermission: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    internal val notificationPermission: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    internal val assistanceState: MutableLiveData<AssistanceCallState> =
        MutableLiveData<AssistanceCallState>()

    private val sdkError: MutableLiveData<String> = MutableLiveData<String>()
    private val sdkWarning: MutableLiveData<String> = MutableLiveData<String>()
    private val configError: SingleLiveEvent<MoveConfigurationError> = SingleLiveEvent()

    init {
        moveSdkActivation.addSource(sdkState) {
            moveSdkActivation.value = evaluateWorkingState()
        }
        userId.addSource(sdkState) {
            val id = sharedPref.getString(PREF_USER_ID, "")
            userId.value = id.toString()
            if (BuildConfig.DEBUG) {
                Log.d(this::class.simpleName, "Your user ID: ${id.toString()}")
            }
        }
    }

    private fun evaluateWorkingState(): ActivationState {
        return when (sdkState.value) {
            MoveSdkState.Running -> {
                ActivationState.RUNNING
            }
            else -> {
                ActivationState.NOT_RUNNING
            }
        }
    }

    private lateinit var moveSdkManager: MoveSdkManager

    private lateinit var sharedPref: SharedPreferences

    fun load(context: Context) {
        moveSdkManager = MoveSdkManager.getInstance(context)
        sharedPref = context.getSharedPreferences(PREF_SHARED_NAME, Context.MODE_PRIVATE)

        moveEnabled.postValue(sharedPref.getBoolean(PREF_ENABLED, false))

        moveSdkManager.fetchMoveStateFlow()
            .onEach {
                sdkState.postValue(it)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(this)

        moveSdkManager.fetchTripStateFlow()
            .onEach {
                tripState.postValue(it)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(this)

        moveSdkManager.fetchConfigErrorFlow()
            .onEach {
                configError.postValue(it)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(this)

        moveSdkManager.fetchErrorsFlow()
            .map {
                it.joinToString("\n\n") { moveServiceFailure ->
                    val serviceName = moveServiceFailure.service.name()
                    val reasons =
                        moveServiceFailure.reasons.joinToString("\n") { error -> error.name }
                    "$serviceName\n$reasons"
                }
            }
            .onEach(sdkError::postValue)
            .flowOn(Dispatchers.IO)
            .launchIn(this)

        moveSdkManager.fetchWarningsFlow()
            .map {
                it.joinToString("\n\n") { moveServiceWarning ->
                    val serviceName = moveServiceWarning.service?.name().orEmpty()
                    val reasons =
                        moveServiceWarning.warnings.joinToString("\n") { w -> w.name }
                    "$serviceName\n$reasons"
                }
            }
            .onEach(sdkWarning::postValue)
            .flowOn(Dispatchers.IO)
            .launchIn(this)

        moveSdkManager.fetchAssistanceStateFlow()
            .filterNotNull()
            .onEach(assistanceState::postValue)
            .flowOn(Dispatchers.IO)
            .launchIn(this)
    }

    fun sdkState(): LiveData<MoveSdkState> {
        return sdkState
    }

    fun sdkTripState(): LiveData<MoveTripState> {
        return tripState
    }

    fun userId(): LiveData<String> {
        return userId
    }

    fun sdkError(): LiveData<String> {
        return sdkError
    }

    fun configError(): LiveData<MoveConfigurationError> {
        return configError
    }

    fun sdkWarning(): LiveData<String> {
        return sdkWarning
    }

    fun updatePermissionViews(activity: Activity?) {
        activity?.let {
            locationPermission.postValue(
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
            phoneStatePermission.postValue(
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            )
            overlayPermission.postValue(Settings.canDrawOverlays(it))
            batteryPermission.postValue(isIgnoringBatteryOptimizations(it))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundPermission.postValue(
                    ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                )
                activityPermission.postValue(
                    ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermission.postValue(
                    ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermission.postValue(
                    ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }
        }
    }

    fun requestLocationPermission(activity: Activity?) {
        activity?.let {
            if (ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_DENIED
            ) {
                it.requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }

    fun requestPhoneStatePermission(activity: Activity?) {
        activity?.let {
            if (ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_DENIED
            ) {
                it.requestPermissions(
                    arrayOf(Manifest.permission.READ_PHONE_STATE),
                    PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }

    fun requestOverlayPermission(activity: Activity?) {
        activity?.let {
            if (!Settings.canDrawOverlays(it)) {
                if (ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.SYSTEM_ALERT_WINDOW
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${it.packageName}")
                    )
                    it.startActivityForResult(intent, PERMISSIONS_REQUEST_CODE)
                }
            }
        }
    }

    @SuppressLint("BatteryLife")
    fun requestBatteryPermission(activity: Activity?) {
        activity?.let {
            val ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(it)
            if (!ignoringBatteryOptimizations) {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${it.packageName}")
                )
                it.startActivityForResult(intent, PERMISSIONS_REQUEST_CODE)
            }
        }
    }

    private fun isIgnoringBatteryOptimizations(context: Context?): Boolean {
        context?.let {
            val pm = it.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = it.packageName
            return pm.isIgnoringBatteryOptimizations(packageName)
        }
        return false
    }

    fun requestBackgroundPermission(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity?.let {
                if (ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    it.requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        PERMISSIONS_REQUEST_CODE
                    )
                }
            }
        }
    }

    fun requestActivityPermission(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity?.let {
                if (ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    it.requestPermissions(
                        arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                        PERMISSIONS_REQUEST_CODE
                    )
                }
            }
        }
    }

    fun requestBluetoothPermission(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity?.let {
                if (ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    it.requestPermissions(
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        PERMISSIONS_REQUEST_CODE
                    )
                }
            }
        }
    }

    fun requestNotificationPermission(activity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity?.let {
                if (ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${it.packageName}")
                    )
                    it.startActivityForResult(intent, PERMISSIONS_REQUEST_CODE)
                }
            }
        }
    }

    fun turnMoveSdkOn(context: Context) {
        sharedPref.edit().putBoolean(PREF_ENABLED, true).apply()

        moveSdkManager.moveSdk?.startAutomaticDetection() ?: run {
            moveSdkManager.setupSdk(context, true)
        }
    }

    fun turnMoveSdkOff() {
        sharedPref.edit().putBoolean(PREF_ENABLED, false).apply()

        moveSdkManager.moveSdk?.stopAutomaticDetection()
    }

    fun forceSync() {
        moveSdkManager.moveSdk?.synchronizeUserData()
    }

    fun requestCallAssistance() {
        moveSdkManager.callAssistance()
    }

    fun requestMoveConfigUpdate() {
        moveSdkManager.updateConfig()
    }
}

