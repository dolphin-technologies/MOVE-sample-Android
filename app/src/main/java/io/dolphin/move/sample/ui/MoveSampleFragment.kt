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

import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.dolphin.move.MoveSdk
import io.dolphin.move.MoveSdkState
import io.dolphin.move.MoveTripState
import io.dolphin.move.sample.R


class MoveSampleFragment : Fragment() {

    companion object {
        fun newInstance() = MoveSampleFragment()
    }

    private lateinit var model: MoveSampleViewModel

    override fun onResume() {
        super.onResume()
        model.updatePermissionViews(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.move_sample_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model = ViewModelProvider(requireActivity())[MoveSampleViewModel::class.java]

        val sdkStatusHeader: View = view.findViewById(R.id.sdk_status_header)
        val sdkStatus: TextView = view.findViewById(R.id.sdk_status)
        val sdkAction: SwitchCompat = view.findViewById(R.id.sdk_action)
        val sdkError: TextView = view.findViewById(R.id.sdk_error_text)
        val versionView: TextView = view.findViewById(R.id.sdk_version)

        val locationContainer: View = view.findViewById(R.id.permission_location_container)
        val phoneStateContainer: View = view.findViewById(R.id.permission_phone_state_container)
        val overlayContainer: View = view.findViewById(R.id.permission_overlays_container)
        val batteryContainer: View = view.findViewById(R.id.permission_battery_container)
        val backgroundContainer: View = view.findViewById(R.id.permission_background_container)
        val activityContainer: View = view.findViewById(R.id.permission_activity_container)

        val locationImageView: ImageView = view.findViewById(R.id.permission_location_icon)
        val phoneStateImageView: ImageView = view.findViewById(R.id.permission_phone_state_icon)
        val overlayImageView: ImageView = view.findViewById(R.id.permission_overlays_icon)
        val batteryImageView: ImageView = view.findViewById(R.id.permission_battery_icon)
        val backgroundImageView: ImageView = view.findViewById(R.id.permission_background_icon)
        val activityImageView: ImageView = view.findViewById(R.id.permission_activity_icon)

        val sdkStateView: TextView = view.findViewById(R.id.sdk_state_value)
        val sdkTripStateView: TextView = view.findViewById(R.id.sdk_trip_state_value)
        val sdkContractIdView: TextView = view.findViewById(R.id.sdk_contract_id_value)

        versionView.text = MoveSdk.version

        val swipeRefresh: SwipeRefreshLayout = view.findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            model.forceSync()
            swipeRefresh.isRefreshing = false
        }

        sdkAction.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                model.turnMoveSdkOn(requireContext())
            } else {
                model.turnMoveSdkOff()
            }
        }

        model.moveEnabled.observe(viewLifecycleOwner) {
            sdkAction.isChecked = it
        }

        model.configError().observe(viewLifecycleOwner) {
            it?.let { configurationError ->
                Toast.makeText(requireContext(), configurationError.toString(), Toast.LENGTH_LONG)
                    .show()

                // reset toggle on config error
                sdkAction.isChecked = false
            }
        }

        model.moveSdkActivation.observe(viewLifecycleOwner) { workingState ->
            workingState?.let {
                when (it) {
                    ActivationState.NOT_RUNNING -> {
                        sdkStatus.setText(R.string.state_inactive)
                        sdkStatusHeader.setBackgroundResource(R.drawable.main_header_off_bg)
                    }
                    ActivationState.ERROR -> {
                        sdkStatus.setText(R.string.state_error)
                        sdkStatusHeader.setBackgroundResource(R.drawable.main_header_warn_bg)
                    }
                    ActivationState.RUNNING -> {
                        sdkStatus.setText(R.string.state_active)
                        sdkStatusHeader.setBackgroundResource(R.drawable.main_header_on_bg)
                    }
                }
            }
        }

        model.sdkError().observe(viewLifecycleOwner, { error ->
            if (TextUtils.isEmpty(error)) {
                sdkError.visibility = View.GONE
                sdkError.text = ""
            } else {
                sdkError.visibility = View.VISIBLE
                sdkError.text = error

                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        })

        model.sdkState().observe(viewLifecycleOwner, { moveSdkState ->
            when (moveSdkState) {
                is MoveSdkState.Ready -> {
                    sdkStateView.setText(R.string.sdk_ready)
                }
                is MoveSdkState.Error -> {
                    sdkStateView.setText(R.string.sdk_error)
                }
                is MoveSdkState.Uninitialised -> {
                    sdkStateView.setText(R.string.sdk_uninitialised)
                }
                is MoveSdkState.Running -> {
                    sdkStateView.setText(R.string.sdk_running)
                }
            }
        })

        model.sdkTripState().observe(viewLifecycleOwner, { sdkTripState ->
            when (sdkTripState) {
                MoveTripState.DRIVING -> sdkTripStateView.setText(R.string.sdk_trip_state_driving)
                MoveTripState.HALT -> sdkTripStateView.setText(R.string.sdk_trip_state_halt)
                MoveTripState.IDLE -> sdkTripStateView.setText(R.string.sdk_trip_state_idle)
                MoveTripState.IGNORED -> sdkTripStateView.setText(R.string.sdk_trip_state_ignored)
                else -> {
                    // UNKNOWN
                    sdkTripStateView.setText(R.string.sdk_trip_state_unknown)
                }
            }
        })

        model.contractId().observe(viewLifecycleOwner, { contractId ->
            if (contractId.isNullOrEmpty()) {
                sdkContractIdView.setText(R.string.not_registered)
            } else {
                sdkContractIdView.text = contractId
            }
        })

        val locationContainerClick = View.OnClickListener {
            model.requestLocationPermission(activity)
        }
        locationContainer.setOnClickListener(locationContainerClick)

        model.locationPermission.observe(viewLifecycleOwner, { granted ->
            locationImageView.setImageResource(getPermissionIcon(granted))
        })

        val phoneStateContainerClick = View.OnClickListener {
            model.requestPhoneStatePermission(activity)
        }
        phoneStateContainer.setOnClickListener(phoneStateContainerClick)

        model.phoneStatePermission.observe(viewLifecycleOwner, { granted ->
            phoneStateImageView.setImageResource(getPermissionIcon(granted))
        })

        val overlayContainerClick = View.OnClickListener {
            model.requestOverlayPermission(activity)
        }
        overlayContainer.setOnClickListener(overlayContainerClick)

        model.overlayPermission.observe(viewLifecycleOwner, { granted ->
            overlayImageView.setImageResource(getPermissionIcon(granted))
        })

        val batteryContainerClick = View.OnClickListener {
            model.requestBatteryPermission(activity)
        }
        batteryContainer.setOnClickListener(batteryContainerClick)

        model.batteryPermission.observe(viewLifecycleOwner, { granted ->
            batteryImageView.setImageResource(getPermissionIcon(granted))
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundContainer.setOnClickListener {
                model.requestBackgroundPermission(activity)
            }
            model.backgroundPermission.observe(viewLifecycleOwner, { granted ->
                backgroundImageView.setImageResource(getPermissionIcon(granted))
            })
            activityContainer.setOnClickListener {
                model.requestActivityPermission(activity)
            }
            model.activityPermission.observe(viewLifecycleOwner, { granted ->
                activityImageView.setImageResource(getPermissionIcon(granted))
            })
        } else {
            // Not required < API 29
            backgroundContainer.visibility = View.GONE
            activityContainer.visibility = View.GONE
        }

        model.load(requireActivity())
    }

    private fun getPermissionIcon(granted: Boolean): Int {
        return if (granted)
            R.drawable.icon_active
        else
            R.drawable.icon_inactive
    }
}